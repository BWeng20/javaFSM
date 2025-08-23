import com.bw.fsm.Data;
import com.bw.fsm.FsmExecutor;
import com.bw.fsm.ScxmlSession;
import com.bw.fsm.actions.Action;
import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.datamodel.ecma.ECMAScriptDatamodel;
import com.bw.fsm.datamodel.null_datamodel.NullDatamodel;
import com.bw.fsm.tracer.TraceMode;

import java.net.URL;
import java.util.List;

public class CustomActions {

    public static class MyAction implements Action {

        @Override
        public Data execute(List<Data> arguments, GlobalData global) {
            int i = 0;
            System.out.printf("MyAction called with %d arguments:\n", arguments.size());
            for (var data : arguments) {
                i += 1;
                System.out.printf("\t%d: %s\n", i, data);
            }
            return Data.Boolean.TRUE;
        }
    }

    public static void main(String[] args) {
        System.out.println("""
                
                Action Example
                -----------------------------------------
                The FSM will call two custom actions from different places.
                These actions can be called at any element that containt expressions or executable content.
                """);

        // Register Data Models
        ECMAScriptDatamodel.register();
        NullDatamodel.register();

        // Create the wrapper to store our actions.
        var actions = new ActionWrapper();

        // We register the same function with different names.
        var my_action = new MyAction();
        actions.add_action("myEnterAction", my_action);
        actions.add_action("myLeaveAction", my_action);

        try {

            // Create the fsm-executor.
            // In this example we don't need any io-processor.
            // Otherwise: var executor = new FsmExecutor(true);
            var executor = new FsmExecutor(false);

            // Start the FSM. Executor has different alternative
            // of this execute-methode. You can load the FSM also from memory,
            // or add some data to initialize the data-model.
            URL source = CustomActions.class.getResource("CustomActions.scxml");
            ScxmlSession session = executor.execute(source.toURI().toString(), actions,
                    // If Trace feature is enabled, we can trigger additional output about
                    // states and transitions. See TraceMode for the different modes.
                    // The Trace feature is designed to be used for external monitoring of
                    // the FSM, here it will only print the state transitions.
                    TraceMode.ALL);

            // The FSM now runs in some other thread.
            // We could send events to the session via session.sender.
            // As we have nothing else to do here... we wait.
            // The example fsm will terminate after some timeout.
            do {
                try {
                    System.out.println("FSM started. Waiting to terminate...");
                    session.thread.join();
                } catch (Exception e) {
                    System.out.println("Join interrupted...");
                    // Here you need to check if there is any reason that you were interrupted,e.g.
                    // because your systems needs to shut down.
                }
            } while (session.thread.isAlive());
            System.out.println("FSM terminated!");

            // Here you could check the results via the session.
            // E.g. check the final reached states (the set of active states is called the "configuration", normally this is on of the "final" states and their parents).
            System.out.println( "Final Configuration: "+session.global_data.final_configuration );

        } catch (Exception e) {
            System.err.println("Failed to execute: " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}
