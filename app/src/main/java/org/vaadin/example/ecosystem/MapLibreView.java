package org.vaadin.example.ecosystem;

import org.vaadin.addons.maplibre.MapLibre;

import com.vaadin.flow.router.Route;

/**
 * maplibre (org.parttio:maplibre 2.1.1). Not supported: the map setup is a
 * script built at runtime. The view only shows the evidence.
 */
@Route("eco/maplibre")
public class MapLibreView extends EcoView {

    public MapLibreView() {
        super("maplibre");
        run(() -> {
            MapLibre map = withId(new MapLibre(), "map");
            map.setHeight("300px");
            map.setCenter(22.3, 60.45);
            map.setZoomLevel(5);
            add(map);
            map.addMarker(22.3, 60.45);
        });
    }
}
