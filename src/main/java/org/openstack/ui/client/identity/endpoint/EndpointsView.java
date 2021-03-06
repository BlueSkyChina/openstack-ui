package org.openstack.ui.client.identity.endpoint;

import java.util.List;
import java.util.Set;

import org.openstack.admin.client.Administration;
import org.openstack.model.identity.Endpoint;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

public class EndpointsView extends Composite {

	private static Binder uiBinder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, EndpointsView> {
	}
	
	public interface Presenter {
		void onCreate();
		void onDelete();
		void onRefresh();
	}
	
	@UiField Button create;
	@UiField Button delete;
	@UiField Button refresh;
	
	@UiField(provided = true) DataGrid<Endpoint> grid;
	
	MultiSelectionModel<Endpoint> selectionModel;
	
	DefaultSelectionEventManager<Endpoint> selectionEventManager = DefaultSelectionEventManager.createCheckboxManager(0);
	
	AsyncDataProvider<Endpoint> asyncDataProvider = new AsyncDataProvider<Endpoint>() {

		@Override
		protected void onRangeChanged(HasData<Endpoint> display) {
			
			final Range range = display.getVisibleRange();
			
			Administration.CLOUD.listEndpoints(new AsyncCallback<List<Endpoint>>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert(caught.getMessage());
					
				}

				@Override
				public void onSuccess(List<Endpoint> result) {
					update();
					updateRowData(range.getStart(), result);
					updateRowCount(range.getLength(), true);
					
				}
			});
			
		}

	};
	
	Presenter presenter;

	public EndpointsView() {
		createGrid();
		initWidget(uiBinder.createAndBindUi(this));
	}

	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
	
	public void refresh() {
		grid.setVisibleRangeAndClearData(grid.getVisibleRange(), true);
		//RangeChangeEvent.fire(grid, grid.getVisibleRange());
	}

	@UiHandler("create")
	void onCreateClick(ClickEvent event) {
		presenter.onCreate();
	}
	
	@UiHandler("delete")
	void onDeleteClick(ClickEvent event) {
		Set<Endpoint> users = selectionModel.getSelectedSet();
		String[] ids = Collections2.transform(users, new Function<Endpoint, String>() {

			@Override
			public String apply(Endpoint user) {
				return user.getId();
			}
			
		}).toArray(new String[0]);
		Administration.CLOUD.deleteEndpoints(ids, new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				refresh();
				presenter.onDelete();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert(caught.toString());
			}
			
		});
	}
	
	@UiHandler("refresh")
	void onRefreshClick(ClickEvent event) {
		//grid.setVisibleRangeAndClearData(grid.getVisibleRange(), true);
		RangeChangeEvent.fire(grid, grid.getVisibleRange());
		presenter.onRefresh();
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
		grid = new DataGrid<Endpoint>();
		selectionModel = new MultiSelectionModel<Endpoint>();
		Column<Endpoint, Boolean> checkboxColumn = new Column<Endpoint, Boolean>(new CheckboxCell()) {

			@Override
			public Boolean getValue(Endpoint object) {
				return selectionModel.isSelected(object);
			}
		};
		grid.setColumnWidth(checkboxColumn, "30px");
		grid.addColumn(checkboxColumn, "");
		TextColumn<Endpoint> typeColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return ""; //object.getType();
			}
		};
		/*
		TextColumn<Endpoint> idColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return object.getId();
			}
		};
		grid.setColumnWidth(idColumn, "120px");
		grid.addColumn(idColumn, "Id");
		*/
		TextColumn<Endpoint> serviceIdColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return object.getServiceId();
			}
		};
		grid.setColumnWidth(serviceIdColumn, "120px");
		grid.addColumn(serviceIdColumn, "Service Id");

		TextColumn<Endpoint> regionColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return ""; //object.getName();
			}
		};
		grid.setColumnWidth(regionColumn, "80px");
		grid.addColumn(regionColumn, "Region");
		TextColumn<Endpoint> publicURLColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return object.getPublicURL();
			}
		};
		grid.setColumnWidth(publicURLColumn, "130px");
		grid.addColumn(publicURLColumn, "Public URL");
		TextColumn<Endpoint> internalURLColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return object.getInternalURL();
			}
		};
		grid.setColumnWidth(internalURLColumn, "130px");
		grid.addColumn(internalURLColumn, "Internal URL");
		TextColumn<Endpoint> administrationURLColumn = new TextColumn<Endpoint>() {
			@Override
			public String getValue(Endpoint object) {
				return object.getAdminURL();
			}
		};
		grid.setColumnWidth(administrationURLColumn, "130px");
		grid.addColumn(administrationURLColumn, "Administration URL");
		grid.setSelectionModel(selectionModel, selectionEventManager);
		selectionModel.addSelectionChangeHandler(new Handler() {
			
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				update();
				
			}
		});
		asyncDataProvider.addDataDisplay(grid); 
	}
	
}


