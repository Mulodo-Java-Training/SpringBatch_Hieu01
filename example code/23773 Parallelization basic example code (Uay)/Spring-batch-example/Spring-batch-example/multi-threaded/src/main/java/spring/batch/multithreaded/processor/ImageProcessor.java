package spring.batch.multithreaded.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.imgscalr.Scalr;
import org.springframework.batch.item.ItemProcessor;

import spring.batch.multithreaded.domain.ImageSubmission;
import spring.batch.multithreaded.domain.ProcessedImage;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;

public class ImageProcessor implements ItemProcessor<ImageSubmission, ProcessedImage> {
	private static final Log LOG = LogFactory.getLog(ImageProcessor.class);

	private final int width;
	private final int height;
	private final String path;

	public ImageProcessor(final String path, final int width, final int height) {
		this.path = path;
		this.width = width;
		this.height = height;
	}

	public ProcessedImage process(ImageSubmission imageSubmission) throws Exception {
		final String imagePath = path + imageSubmission.getFileName();

		try {
			final BufferedImage originalImage = ImageIO.read(new File(imagePath));
			final BufferedImage resizedImage = Scalr.resize(originalImage, width, height);

			return new ProcessedImage(resizedImage, imageSubmission.getFileName());
		} catch (Exception e) {
			LOG.warn("Failed to convert " + imagePath + ", skipping - sorry submitter! (" + e.getMessage() + ")");

			return null;
		}
	}
}
