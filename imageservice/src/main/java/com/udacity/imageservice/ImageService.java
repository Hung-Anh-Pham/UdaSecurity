package com.udacity.imageservice;

import java.awt.image.BufferedImage;

public interface ImageService {

    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold);

}
