module fr.alescis.aelia {
    requires java.net.http;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.web;
    requires jdk.jsobject;
    requires atlantafx.base;

    exports fr.alescis.aelia;
    exports fr.alescis.aelia.model;
    exports fr.alescis.aelia.provider;
    exports fr.alescis.aelia.service;

    opens fr.alescis.aelia.ui.components to javafx.web;
}
