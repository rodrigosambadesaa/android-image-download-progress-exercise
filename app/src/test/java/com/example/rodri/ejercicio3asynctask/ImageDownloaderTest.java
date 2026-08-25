package com.example.rodri.ejercicio3asynctask;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImageDownloaderTest {

    @Test
    public void fileNameFromUrlUsesLastPathSegment() {
        assertEquals(
                "fachada.jpg",
                ImageDownloader.fileNameFromUrl(
                        "https://tesdai.com/images/tesdai/fachada.jpg"));
    }

    @Test
    public void fileNameFromUrlFallsBackWhenPathEndsWithSlash() {
        assertEquals(
                "downloaded-image.jpg",
                ImageDownloader.fileNameFromUrl("https://example.com/"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHttpSchemes() {
        ImageDownloader.validateHttpUrl("file:///tmp/image.jpg");
    }
}
