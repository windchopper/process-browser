package name.wind.tools.process.browser;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import name.wind.common.search.Search;
import name.wind.common.search.SearchContinuation;
import name.wind.common.search.SearchStoppedException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class WidgetSearch extends Search {

    public WidgetSearch() {
        super(asList(
            object -> object instanceof Scene ? singletonList(((Scene) object).getRoot()) : emptyList(),
            object -> object instanceof ContextMenu ? ((ContextMenu) object).getItems() : emptyList(),
            object -> object instanceof Pane ? ((Pane) object).getChildren() : emptyList(),
            object -> object instanceof Control ? singletonList(((Control) object).getContextMenu()) : emptyList()
        ));
    }

    public static class Continuation implements SearchContinuation<Object> {

        private Object searchResult;

        @Override public Object newContext() {
            searchResult = null;
            return null;
        }

        @Override public Object deriveContext(Object context, Object tail) {
            return null;
        }

        @Override public void found(Object context, Object found) throws SearchStoppedException {
            searchResult = found;
            throw new SearchStoppedException();
        }

        public Object searchResult() {
            return searchResult;
        }

    }

}
