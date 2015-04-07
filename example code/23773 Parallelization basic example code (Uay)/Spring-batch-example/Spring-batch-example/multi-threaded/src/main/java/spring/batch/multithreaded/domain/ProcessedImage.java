package spring.batch.multithreaded.domain;

import java.awt.image.BufferedImage;

public class ProcessedImage {
	private final String fileName;
	private final BufferedImage image;

	public ProcessedImage(final BufferedImage image, final String fileName) {
		this.image = image;
		this.fileName = fileName;
	}

	public BufferedImage getImage() {
		return image;
	}

	public String getFileName() {
		return fileName;
	}
}
