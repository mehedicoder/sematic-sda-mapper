package com.sda.mapper.ui.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

import java.util.List;

/**
 * A reusable search bar component with a text field and entity type filter.
 * Fires a {@link SearchEvent} whenever the user types or selects a type.
 */
@Tag("search-bar")
public class SearchBar extends HtmlComponent {

    private final TextField searchField;
    private final ComboBox<String> typeFilter;

    public SearchBar() {
        searchField = new TextField();
        searchField.setPlaceholder("Search ontology entities...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();

        typeFilter = new ComboBox<>("Type");
        typeFilter.setItems(List.of(
                "All", "Requirement", "Capability", "Skill",
                "Service", "CommunicationBinding", "DataConnector", "DeploymentContext"
        ));
        typeFilter.setValue("All");
        typeFilter.setWidth("220px");

        searchField.addValueChangeListener(e -> fireEvent(new SearchEvent(this, getQuery(), getType())));
        typeFilter.addValueChangeListener(e -> fireEvent(new SearchEvent(this, getQuery(), getType())));

        var layout = new HorizontalLayout(searchField, typeFilter);
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.END);
        layout.setSpacing(true);
        layout.setPadding(false);

        getElement().appendChild(layout.getElement());
    }

    public String getQuery() {
        return searchField.getValue();
    }

    public String getType() {
        String val = typeFilter.getValue();
        return val == null || "All".equals(val) ? null : val;
    }

    public void clear() {
        searchField.clear();
        typeFilter.setValue("All");
    }

    public Registration addSearchListener(ComponentEventListener<SearchEvent> listener) {
        return addListener(SearchEvent.class, listener);
    }

    /**
     * Event fired when the user types in the search field or changes the type filter.
     */
    public static class SearchEvent extends ComponentEvent<SearchBar> {
        private final String query;
        private final String entityType;

        public SearchEvent(SearchBar source, String query, String entityType) {
            super(source, false);
            this.query = query;
            this.entityType = entityType;
        }

        public String getQuery() {
            return query;
        }

        public String getEntityType() {
            return entityType;
        }
    }
}
