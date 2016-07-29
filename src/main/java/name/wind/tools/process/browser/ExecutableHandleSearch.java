package name.wind.tools.process.browser;

import name.wind.common.search.Search;
import name.wind.common.search.SearchContinuation;
import name.wind.common.search.SearchStoppedException;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.ProcessModuleHandle;

import java.util.ArrayList;
import java.util.List;

public class ExecutableHandleSearch extends Search<Object> {

    public ExecutableHandleSearch() {
        addInteriorExposer(object -> object instanceof List, object -> (List) object);
        // addInteriorExposer(object -> object instanceof ProcessHandle, object -> ((ProcessHandle) object).modules());
    }

    public static class Continuation implements SearchContinuation<Object, Object> {

        private final List<ProcessHandle> searchResult = new ArrayList<>();

        @Override public Object newContext() {
            searchResult.clear();
            return null;
        }

        @Override public Object deriveContext(Object context, Object tail) {
            return tail;
        }

        @Override public void found(Object context, Object found) throws SearchStoppedException {
            ProcessHandle processHandle = null;

            if (found instanceof ProcessHandle) {
                processHandle = (ProcessHandle) found;
            } else if (found instanceof ProcessModuleHandle) {
                processHandle = (ProcessHandle) context;
            }

            if (processHandle != null) {
                for (ProcessHandle alreadyAddedProcessHandle : searchResult) {
                    if (alreadyAddedProcessHandle.identifier() == processHandle.identifier()) {
                        return;
                    }
                }

                searchResult.add(processHandle);
            }
        }

        public List<ProcessHandle> searchResult() {
            return searchResult;
        }

    }

}
