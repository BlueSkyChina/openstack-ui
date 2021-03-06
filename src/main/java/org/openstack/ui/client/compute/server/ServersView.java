package org.openstack.ui.client.compute.server;

import java.io.Serializable;
import java.util.Set;

import org.openstack.model.compute.Flavor;
import org.openstack.model.compute.Server;
import org.openstack.model.compute.ServerAction;
import org.openstack.model.compute.ServerList;
import org.openstack.model.compute.nova.NovaServerForCreate;
import org.openstack.model.images.Image;
import org.openstack.portal.client.Portal;
import org.openstack.ui.client.common.DefaultAsyncCallback;
import org.openstack.ui.client.common.DefaultGridResources;
import org.openstack.ui.client.common.PreviewButtonCell;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

public class ServersView extends Composite implements CreateServerWizard.Listener, ServerDetails.Listener, ServerActionPicker.Listener {
	
	private static final int POLLING_INTERVAL = 60000;

	private static Binder uiBinder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, ServersView> {
	}
	
	@UiField Button create;
	@UiField Button delete;
	@UiField Button refresh;
	@UiField Button filters;
	@UiField SimplePager pager;
	
	@UiField SimpleLayoutPanel details;
	
	ServerDetails serverDetails = new ServerDetails();
	
	@UiField(provided = true) DataGrid<Server> grid;
	
	MultiSelectionModel<Server> selectionModel;
	
	DefaultSelectionEventManager<Server> selectionEventManager = DefaultSelectionEventManager.createCheckboxManager(0);
	
	private final Timer polling = new Timer() {

		@Override
		public void run() {
			refresh();
		}
		
	};
	
	AsyncDataProvider<Server> asyncDataProvider = new AsyncDataProvider<Server>() {

		@Override
		protected void onRangeChanged(HasData<Server> display) {
			
			polling.cancel();
			
			final Range range = display.getVisibleRange();
			
			Portal.CLOUD.listServers(range.getStart(), range.getLength(),  new DefaultAsyncCallback<ServerList>() {
				@Override
				public void onSuccess(ServerList result) {
					//selectionModel.clear();
					updateRowData(range.getStart(), result.getList());
					updateRowCount(range.getLength(), true);
					update();
					//polling.schedule(POLLING_INTERVAL);
				}
			});
		}

	};

	public ServersView() {
		createGrid();
		initWidget(uiBinder.createAndBindUi(this));
		filters.setVisible(false);
		pager.setVisible(false);
		serverDetails.setListener(this);
	}
	
	public void refresh() {
		grid.setVisibleRangeAndClearData(grid.getVisibleRange(), true);
		//RangeChangeEvent.fire(grid, grid.getVisibleRange());
	}

	@UiHandler("create")
	void onCreateClick(ClickEvent event) {
		CreateServerWizard widget = new CreateServerWizard();
		widget.setListener(this);
		widget.edit(new NovaServerForCreate());
		Portal.MODAL.setWidget(widget);
		Portal.MODAL.center();
	}
	
	@UiHandler("delete")
	void onDeleteClick(ClickEvent event) {
		Set<Server> users = selectionModel.getSelectedSet();
		String[] ids = Collections2.transform(users, new Function<Server, String>() {

			@Override
			public String apply(Server user) {
				return user.getId();
			}
			
		}).toArray(new String[0]);
		Portal.CLOUD.deleteServers(ids, new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				refresh();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert(caught.toString());
			}
			
		});
	}
	
	@UiHandler("refresh")
	void onRefreshClick(ClickEvent event) {
		refresh();
	}
	
	@UiHandler("filters")
	void onFiltersClick(ClickEvent event) {
		ServersFilters widget = new ServersFilters();
		Portal.MODAL.setWidget(widget);
		Portal.MODAL.center();
	}
	
	private void update() {
		switch (selectionModel.getSelectedSet().size()) {
			case 0:
				delete.setEnabled(false);
				break;
			case 1:
				delete.setEnabled(true);
				break;
			default:
				delete.setEnabled(true);
				break;
		}
	}
	
	private void createGrid() {
		DataGrid.Resources resources = GWT.create(DefaultGridResources.class);
		grid = new DataGrid<Server>(15, resources);
		grid.setStyleName(resources.dataGridStyle().dataGridWidget());
		selectionModel = new MultiSelectionModel<Server>();
		Column<Server, Boolean> checkboxColumn = new Column<Server, Boolean>(new CheckboxCell(true, false)) {

			@Override
			public Boolean getValue(Server object) {
				return selectionModel.isSelected(object);
			}
		};
		grid.setColumnWidth(checkboxColumn, "40px");
		grid.addColumn(checkboxColumn, "");
		/*
		Column<Server, String> logoColumn = new Column<Server, String>(new LogoCell()) {

			@Override
			public String getValue(Server object) {
				return "";
			}
		};
		grid.setColumnWidth(logoColumn, "60px");
		grid.addColumn(logoColumn);
		*/
		TextColumn<Server> statusColumn = new TextColumn<Server>() {
			@Override
			public String getValue(Server object) {
				return object.getStatus();
			}
		};
		grid.setColumnWidth(statusColumn, "100px");
		grid.addColumn(statusColumn, "Status");
		TextColumn<Server> nameColumn = new TextColumn<Server>() {
			@Override
			public String getValue(Server object) {
				return object.getName();
			}
		};
		grid.setColumnWidth(nameColumn, "120px");
		grid.addColumn(nameColumn, "Name");
		TextColumn<Server> imageColumn = new TextColumn<Server>() {
			@Override
			public String getValue(Server object) {
				Image image = Portal.images.get(object.getImage().getId());
				return image != null ? image.getName() : "";
			}
		};
		grid.setColumnWidth(imageColumn, "120px");
		grid.addColumn(imageColumn, "Image");
		TextColumn<Server> flavorColumn = new TextColumn<Server>() {
			@Override
			public String getValue(Server object) {
				Flavor flavor = Portal.flavors.get(object.getFlavor().getId());
				return flavor != null ? flavor.getName() : "";
			}
		};
		grid.setColumnWidth(flavorColumn, "120px");
		grid.addColumn(flavorColumn, "Flavor");
		ButtonCell previewButton = new PreviewButtonCell();
		Column<Server,String> preview = new Column<Server,String>(previewButton) {
		  public String getValue(Server object) {
		    return "Preview";
		  }
		};
		preview.setFieldUpdater(new FieldUpdater<Server, String>() {
		  @Override
		  public void update(int index, Server server, String value) {
			  serverDetails.bind(server);
			  details.setWidget(serverDetails);
		  }
		});
		grid.setColumnWidth(preview, "100px");
		grid.addColumn(preview);
		grid.setSelectionModel(selectionModel, selectionEventManager);
		selectionModel.addSelectionChangeHandler(new Handler() {
			
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				update();
				
			}
		});
		
		asyncDataProvider.addDataDisplay(grid); 
	}
	
	@Override
	public void onServerCreated(Server server) {
		refresh();
		Portal.MODAL.hide();
	}

	@Override
	public void onServerActionSuccess(ServerAction action, Serializable result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onServerActionFailure(ServerAction action, Throwable throwable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReboot(Server server) {
		refresh();
	}

	@Override
	public void onServerRebuilt(Server server) {
		refresh();
	}

	@Override
	public void onServerResized(Server server) {
		refresh();
	}

	@Override
	public void onResizeConfirmed(Server server) {
		refresh();
		
	}

	@Override
	public void onResizeReverted(Server server) {
		refresh();
	}

	@Override
	public void onPasswordChanged(Server server) {
		refresh();
		
	}

	@Override
	public void onServerPaused(Server server) {
		refresh();
		
	}

	@Override
	public void onServerUnpaused(Server server) {
		refresh();
		
	}

	@Override
	public void onServerImageCreated() {
		refresh();
	}

	@Override
	public void onServerBackupCreated(Server server) {
		refresh();
	}
	
}


