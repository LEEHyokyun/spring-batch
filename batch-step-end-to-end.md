# 챗지피티에게 부탁하여 마크다운 내용을 구성하였고, 1차적으로 내용이 올바른지 확인 작업을 거친 정리본.

# Spring Batch Job End-to-End 실행 흐름 (최초 실행 기준)

> **목표**
> Application 실행 시점부터 **Job 시작 → Step 실행 → Tasklet 실행 → Step 종료 → Job 종료**까지의 흐름을
> *Spring Batch 내부 클래스 기준*으로 **한 번에 조망**하고, 필요 시 **단계별로 복기**할 수 있도록 정리한다.

---

## 1️⃣ 전체 End-to-End 한눈에 보기 (요약 도식)

```text
[Application 시작]
        |
        v
SpringApplication.run()
        |
        v
ApplicationRunner / CommandLineRunner
        |
        v
JobLauncher.run(job, jobParameters)
        |
        v
SimpleJobLauncher
        |
        v
Job.execute()
        |
        v
AbstractJob.execute()
        |
        +--> JobExecution 생성 (STARTING)
        |
        +--> doExecute()
                |
                v
            SimpleJob.doExecute()
                |
                v
            Step 순차 실행
                |
                v
            Step.execute()
                |
                v
            AbstractStep.execute()
                |
                +--> StepExecution 생성 (STARTING)
                |
                +--> beforeStep()
                |
                +--> doExecute()
                        |
                        v
                    TaskletStep.doExecute()
                        |
                        v
                    RepeatTemplate.iterate()
                        |
                        v
                    StepContextRepeatCallback
                        |
                        v
                    doInChunkContext()
                        |
                        v
                    TransactionTemplate.execute()
                        |
                        v
                    ChunkTransactionCallback.doInTransaction()
                        |
                        v
                    Tasklet.execute()
                        |
                        v
                    RepeatStatus 반환
                        |
                        v
            StepExecution 상태 업데이트 (COMPLETED / FAILED)
                |
                v
        JobExecution 상태 업데이트 (COMPLETED / FAILED)
                |
                v
[Application 종료]
```

---

## 2️⃣ 단계별 상세 흐름 (실행 → 실행 → 종료 → 종료)

---

## 🟢 1단계: Application → Job 실행 시작

```text
SpringApplication.run()
   |
   v
ApplicationRunner / CommandLineRunner
   |
   v
JobLauncher.run(Job, JobParameters)
```

* Spring Boot는 **ApplicationRunner / CommandLineRunner**를 통해 배치 실행 진입
* `spring.batch.job.enabled=true` 또는 직접 `JobLauncher` 호출

---

## 🟢 2단계: Job 실행 (SimpleJob / AbstractJob)

```text
SimpleJobLauncher.run()
   |
   v
Job.execute()
   |
   v
AbstractJob.execute()
   |
   +--> JobExecution 생성
   |        상태: STARTING
   |
   +--> validate()
   |
   +--> doExecute()
```

### 핵심 포인트

* `AbstractJob.execute()`는 **템플릿 메서드**
* 실제 Step 실행 로직은 `doExecute()`에 위임

---

## 🟢 3단계: Step 실행 (AbstractStep)

```text
SimpleJob.doExecute()
   |
   v
for (Step step : steps)
   |
   v
Step.execute()
   |
   v
AbstractStep.execute()
   |
   +--> StepExecution 생성
   |        상태: STARTING
   |
   +--> JobRepository.update(stepExecution)
   |
   +--> beforeStep()  (StepExecutionListener)
   |
   +--> doExecute(stepExecution)
```

### 🔍 AbstractStep.execute() 내부 핵심 포인트

* **트랜잭션 경계는 Step이 아니라 Step 구현체(TaskletStep / ChunkOrientedStep)에 위임**
* `execute()`는 *라이프사이클 관리 + 상태 관리* 역할
* 실제 비즈니스 실행은 반드시 `doExecute()`에서 발생

---

## 🟢 4단계: TaskletStep.doExecute() 상세 (Chunk / Transaction / Callback)

```text
TaskletStep.doExecute(stepExecution)
   |
   v
RepeatTemplate.iterate(
   └─ StepContextRepeatCallback
         |
         v
     doInChunkContext()
         |
         v
     TransactionTemplate.execute()
         |
         v
     ChunkTransactionCallback.doInTransaction()
         |
         v
     Tasklet.execute()
```

---

### 🔥 핵심 콜백 체인 구조

```text
RepeatTemplate
  └─ RepeatCallback
       └─ StepContextRepeatCallback
            └─ ChunkTransactionCallback
                 └─ Tasklet.execute()
```

---

### 4-1️⃣ doInChunkExecution (RepeatTemplate)

```text
RepeatTemplate.iterate()
   |
   v
RepeatCallback.doInIteration()
   |
   v
StepContextRepeatCallback.doInChunkContext()
```

* StepExecutionContext를 **Chunk 단위로 바인딩**
* 반복 실행 가능 구조 (RepeatStatus.CONTINUABLE)
* Tasklet / Chunk 공통 기반 구조

---

### 4-2️⃣ doInTransaction (TransactionTemplate)

```text
TransactionTemplate.execute()
   |
   v
ChunkTransactionCallback.doInTransaction()
```

* **실질적인 트랜잭션 시작 지점**
* PlatformTransactionManager 사용
* 예외 발생 시 Rollback

---

### 4-3️⃣ Tasklet.execute() 호출

```text
Tasklet.execute(StepContribution, ChunkContext)
   |
   v
RepeatStatus 반환
   - FINISHED
   - CONTINUABLE
```

* Tasklet의 비즈니스 로직 진입 지점
* 반환값에 따라 RepeatTemplate 반복 여부 결정

---

## 🔵 5단계: Step 종료

```text
Tasklet.execute() 종료
   |
   v
StepExecution.apply(contribution)
   |
   v
StepExecution 상태 변경
   - COMPLETED / FAILED
   |
   v
afterStep() (StepExecutionListener)
```

---

## 🔵 6단계: Job 종료

```text
모든 Step 완료
   |
   v
JobExecution 상태 변경
   - COMPLETED / FAILED
   |
   v
JobRepository.update(jobExecution)
```

---

## 🟢 4단계: Tasklet 실행 + 트랜잭션

```text
TaskletStep.doExecute()
   |
   v
TransactionTemplate.execute()
   |
   v
Tasklet.execute(StepContribution, ChunkContext)
   |
   v
RepeatStatus 반환
```

### 🔥 핵심 메서드

* `TaskletStep.doExecute()`
* `doInTransaction()` 내부에서 실행됨

### 트랜잭션 의미

* **Tasklet 단위 = 트랜잭션 단위**
* 예외 발생 시 Rollback

---

## 🔵 5단계: Step 종료

```text
Tasklet.execute() 종료
   |
   v
StepExecution 상태 변경
   - COMPLETED / FAILED
   |
   v
afterStep()
```

* Step 상태가 Job의 흐름 제어 기준
* FAILED 시 Job 중단 가능

---

## 🔵 6단계: Job 종료

```text
모든 Step 완료
   |
   v
JobExecution 상태 변경
   - COMPLETED / FAILED
   |
   v
JobRepository 업데이트
```

* JobExecution은 **Job의 최종 결과**
* ExitStatus / BatchStatus 확정

---

## 3️⃣ 핵심 클래스 관계 요약

```text
Application
  └─ JobLauncher (SimpleJobLauncher)
       └─ Job (SimpleJob)
            └─ AbstractJob.execute()
                 └─ Step (TaskletStep)
                      └─ AbstractStep.execute()
                           └─ Tasklet.execute()
```

---

## 4️⃣ 복기용 핵심 문장 요약

* **Job / Step은 모두 템플릿 메서드 구조**
* `execute()`는 공통 흐름, `doExecute()`가 실제 구현
* Tasklet은 **트랜잭션 내부에서 실행**
* Step의 성공/실패가 Job의 흐름을 결정
* JobExecution / StepExecution은 상태 머신 역할

---

> 📌 이 문서는 `README.md`에 그대로 붙여 사용 가능
> 복기 시에는 **2 → 3 → 4단계**를 특히 집중해서 보는 것을 추천
