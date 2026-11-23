package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
import com.vaadin.flow.router.Route;

/**
 * ImageCrop (com.flowingcode.vaadin.addons:image-crop-addon 1.3.0).
 */
@Route("eco/image-crop")
public class ImageCropView extends EcoView {

    public ImageCropView() {
        super("ImageCrop");
        ImageCrop crop = withId(new ImageCrop("images/eco-sample.png"), "crop");
        crop.getElement().getStyle().set("width", "400px");
        run(() -> crop.setCrop(new Crop("%", 10, 10, 50, 50)));
        add(crop);
    }
}
