package name.wind.tools.process.browser;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import name.wind.common.search.Search;
import name.wind.common.search.SearchContinuation;
import name.wind.common.search.SearchStoppedException;

import static java.util.Collections.singletonList;

public class WidgetSearch extends Search<Object> {

    public WidgetSearch() {
        addInteriorExposer(object -> object instanceof Scene, object -> singletonList(((Scene) object).getRoot()));
        addInteriorExposer(object -> object instanceof ContextMenu, object -> ((ContextMenu) object).getItems());
        addInteriorExposer(object -> object instanceof Pane, object -> ((Pane) object).getChildren());
        addInteriorExposer(object -> object instanceof Control, object -> singletonList(((Control) object).getContextMenu()));
    }

    public static class Continuation implements SearchContinuation<Object, Object> {

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
