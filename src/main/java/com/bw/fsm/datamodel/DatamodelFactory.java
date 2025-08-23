package com.bw.fsm.datamodel;

import com.bw.fsm.Log;
import com.bw.fsm.datamodel.null_datamodel.NullDatamodel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class DatamodelFactory {

    /// Create a NEW datamodel.
    public abstract Datamodel create(GlobalData global_data, Map<String, String> options);

    private static final Map<String, DatamodelFactory> datamodel_factories = new HashMap<>();

    /**
     * Register a new Datamodel.\
     * The name is case-insensitive.
     */
    public static void register_datamodel(String name, DatamodelFactory factory) {
        datamodel_factories.put(name.toLowerCase(Locale.CANADA), factory);
    }

    @NotNull
    public static Datamodel create_datamodel(
            String name, GlobalData global_data,
            Map<String, String> options
    ) {
        DatamodelFactory factory = datamodel_factories.get((name == null || name.isEmpty()) ? "null" : name.toLowerCase(Locale.CANADA));
        if (factory != null) {
            return factory.create(global_data, options);
        } else {
            Log.panic("Unsupported Data Model '%s'", name);
            return datamodel_factories.get(NullDatamodel.NULL_DATAMODEL).create(global_data, options);
        }
    }
}
