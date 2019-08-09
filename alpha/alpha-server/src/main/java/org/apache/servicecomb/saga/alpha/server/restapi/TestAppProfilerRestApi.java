package org.apache.servicecomb.saga.alpha.server.restapi;

import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestAppProfilerRestApi {
	private static final Logger LOG = LoggerFactory.getLogger(TestAppProfilerRestApi.class);
	private static final Gauge TXLE_METRIC_TEST = Gauge.build("txle_metric_test", "test metric help").register();

	@GetMapping("/testMetric")
	public String testMetric() {
		TXLE_METRIC_TEST.inc();
		return TXLE_METRIC_TEST.get() + "";
	}

	@GetMapping("/cpu/increaseUsage")
	public void increaseCPUUsage() {
		long a = System.currentTimeMillis();
		for (int i = 1; i < 1000001; i++) {
			System.out.println(i);
		}
		System.out.println("Executing increaseCPUUsage method took " + (System.currentTimeMillis() - a) + " ms.");
	}

	@GetMapping("/test")
	public void test() {
//		Integer.parseInt("");
		Map<String, String> map1 = new HashMap<>();
		Map<String, String> map2 = new HashMap<>();
		Map<String, String> map3 = new HashMap<>();

		Map<String, String> map = new HashMap<>();
		map.put("a", "test gc.");
		map.keySet().forEach(key -> {
			System.out.println(key + " = " + map.get(key));
		});

		new Thread(){
			@Override
			public void run() {
				System.out.println(System.currentTimeMillis());
				try {Thread.sleep(60 * 1000);} catch (InterruptedException e) {}
			}
		}.start();
	}

}
