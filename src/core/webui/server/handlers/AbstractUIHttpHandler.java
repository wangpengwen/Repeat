package core.webui.server.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;

import core.ipc.IIPCService;
import core.ipc.IPCServiceManager;
import core.keyChain.TaskActivation;
import core.userDefinedTask.TaskGroup;
import core.webcommon.HttpServerUtilities;
import core.webui.server.handlers.renderedobjects.ObjectRenderer;
import core.webui.server.handlers.renderedobjects.RenderedActivation;
import core.webui.server.handlers.renderedobjects.RenderedIPCService;
import core.webui.server.handlers.renderedobjects.RenderedTaskGroup;
import core.webui.server.handlers.renderedobjects.RenderedUserDefinedAction;

public abstract class AbstractUIHttpHandler extends AbstractSingleMethodHttpHandler {
	protected ObjectRenderer objectRenderer;

	public AbstractUIHttpHandler(ObjectRenderer objectRenderer, String allowedMethod) {
		super(allowedMethod);
		this.objectRenderer = objectRenderer;
	}

	protected Void renderedIpcServices(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		List<RenderedIPCService> services = new ArrayList<>(IPCServiceManager.IPC_SERVICE_COUNT);
		for (int i = 0; i < IPCServiceManager.IPC_SERVICE_COUNT; i++) {
			IIPCService service = IPCServiceManager.getIPCService(i);
			services.add(RenderedIPCService.fromIPCService(service));
		}
		data.put("ipcs", services);

		return renderedPage(exchange, "rendered_ipcs", data);
	}

	protected Void renderedTaskForGroup(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		TaskGroup group = backEndHolder.getCurrentTaskGroup();
		List<RenderedUserDefinedAction> taskList = group.getTasks().stream().map(RenderedUserDefinedAction::fromUserDefinedAction).collect(Collectors.toList());
		data.put("tasks", taskList);

		return renderedPage(exchange, "rendered_tasks", data);
	}

	protected Void renderedTaskGroups(HttpAsyncExchange exchange) throws IOException {
		Map<String, Object> data = new HashMap<>();
		data.put("groups", backEndHolder.getTaskGroups()
			.stream().map(g -> RenderedTaskGroup.fromTaskGroup(g, g == backEndHolder.getCurrentTaskGroup()))
			.collect(Collectors.toList()));

		return renderedPage(exchange, "rendered_task_groups", data);
	}

	protected Void renderedKeyChains(HttpAsyncExchange exchange, TaskActivation activation) throws IOException {
		return renderedPage(exchange, "rendered_key_chains", getRenderedTaskActivationData(activation));
	}

	protected Void renderedKeySequences(HttpAsyncExchange exchange, TaskActivation activation) throws IOException {
		return renderedPage(exchange, "rendered_key_sequences", getRenderedTaskActivationData(activation));
	}

	protected Void renderedPhrases(HttpAsyncExchange exchange, TaskActivation activation) throws IOException {
		return renderedPage(exchange, "rendered_phrases", getRenderedTaskActivationData(activation));
	}

	private Map<String, Object> getRenderedTaskActivationData(TaskActivation activation) {
		Map<String, Object> data = new HashMap<>();
		data.put("activation", RenderedActivation.fromActivation(activation));
		return data;
	}

	protected Void renderedPage(HttpAsyncExchange exchange, String template, Map<String, Object> data) throws IOException {
		String page = objectRenderer.render(template, data);
		if (page == null) {
			return HttpServerUtilities.prepareHttpResponse(exchange, 500, "Failed to render page.");
		}

		return HttpServerUtilities.prepareHttpResponse(exchange, HttpStatus.SC_OK, page);
	}
}
