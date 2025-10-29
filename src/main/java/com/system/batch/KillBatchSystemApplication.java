package com.system.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KillBatchSystemApplication {

	public static void main(String[] args) {
		/*
		* 배치 작업의 성공/실패 상태를 exit code로 외부 시스템에 전달
		* 실무에서 배치 모니터링과 제어에 필수적
		* */
		System.exit(SpringApplication.exit(SpringApplication.run(KillBatchSystemApplication.class, args)));
	}

}
