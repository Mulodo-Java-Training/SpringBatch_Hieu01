package remote.partition.slave;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JobRunner {
	private static final String CONFIG = "/META-INF/spring/config/job-context.xml";
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		new ClassPathXmlApplicationContext(CONFIG);
	}
}
