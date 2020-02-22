package com.sam.mdm.model;

import java.util.List;

public class Saga {
	private List<Step> steps;
	private SAGAModel sagaModel;

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public SAGAModel getSagaModel() {
		return sagaModel;
	}

	public void setSagaModel(SAGAModel sagaModel) {
		this.sagaModel = sagaModel;
	}

	public static class Step {
		private String name;
		private API api;
		private API capi;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name.trim();
		}

		public API getApi() {
			return api;
		}

		public void setApi(API api) {
			this.api = api;
		}

		public API getCapi() {
			return capi;
		}

		public void setCapi(API capi) {
			this.capi = capi;
		}

		public static class API {
			private String path;
			private String method;

			public String getMethod() {
				return method;
			}

			public void setMethod(String method) {
				this.method = method.trim();
			}

			public String getPath() {
				return path;
			}

			public void setPath(String path) {
				this.path = path.trim();
			}

		}
	}
}