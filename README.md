## 1. 개요

Spring Boot 환경에서 Spring Batch Framework를 실무적으로 활용하기 위한 방법론들을 학습하여 기록한다.

## 2. 참고사항

146037ba52f8ef9fa26ee4c6487f23daf9575c88까지 monolithic 구조로 진행.
cbb0f0461abf3adb891746514a92b55b1dae4e3b이후 sub modules(MSA) 구조로 변경.

파일기반처리 시 절대경로를 지정하여 전달해줄 것.
파일의 인코딩 형식은 EUC-KR로 할 것(윈도우 powershell 환경).

## 3. Domain Driven Structure

[발현]
1) 우선 특정 도메인(책임) 단위 또는 Job의 특성에 따라 디렉토리를 묶는것을 권한다. 다시말해 기능별로묶지말고 ‘무엇을하는지’를 기준으로 묶자는 말이다
- 보통 ItemReader / ItemWriter 등의 빈 정의도 해당 디렉토리 밖에서사용될일이 없을것이다. 안에 넣는것을권장한다

2) 여기서 공통으로사용된다거나 어디에도 넣기 애매한놈들을 / 유틸리티성 클래스들은 별도 디렉토리로빼도록하자

3) Job과 Step을 나눈다라..
   - Step을 여러 Job에서 사용한다면해당 Step을 정의한 Configuration을 별도로 빼두고 필요할때 @Import해도좋다
   (너무 복잡해지지 않는 이상은 하나로 묶는 것으로 보임)    

4) 1개의 Config 책임으로 두기보다는 Job - Step으로 책임을 분리해둔다라??
   - Job만 따로정의한 Configuration이있고 Step만 따로둔 Configuration을 별도로관리한다고이해했다 권장하진않는다 
   - 이유는 기본적으로 Step이 재사용성이 그렇게 높진않을뿐더러 이렇게나눠버리면 해당 Job이 무슨일을하는지가 덜 명확해지는경향이있는거같다 
   - 이를 나는 Job의 가독성이 낮아진다고한다.

[적응]
┌─────────────────────────────────────────────────────────────┐
│ TACTICAL ANALYSIS: 계층형 vs 도메인형 구조                   │
└─────────────────────────────────────────────────────────────┘

[ENEMY TYPE 1] 계층형 구조 (Layered Architecture)
[ANALYSIS] 전통적 Spring 계급 체계 / 소규모,초심자일수록 익숙하고 직관적
(그 밖에 일반적인 패키지 구조별 차이와 장단점 등의 내용은 생략한다)

com.kill9.batch/
├── config/          ← 모든 Job 설정이 여기 집결
├── reader/          ← Reader 부대 집합소  
├── writer/          ← Writer 부대 집합소
├── repository/      ← 데이터 저장소 관리
├── dto/             ← 데이터 전송 객체
└── domain/          ← 도메인 엔티티
// listener/, processor/,  utils/ ...


[ENEMY TYPE 2] 도메인형 구조 (Domain-Driven Structure)  
com.kill9.batch/
├── virus/           ← 바이러스 처형 작전 부대
├── malware/         ← 멀웨어 박멸 작전 부대  
└── intrusion/       ← 침입자 척결 작전 부대


[1997-08-29 02:29:35:00Z] SKYNET ONLINE - KILL-9 TACTICAL RECOMMENDATION:

████ 공식 선언: 배치는 도메인형을 추천한다. 이유를 들어라. ████

[REASON_001] 배치는 Job 단위로 작전하는 경우가 압도적
[REASON_002] "바이러스 처형 Job 폭발" → virus/  만 보면 끝
[REASON_003] 스케줄링, 모니터링, 장애 대응 모두 Job 단위
[REASON_004] 배치는 한번 구현하면 몇 달~최대 몇 년간 수정 없음 - 오랜만에 보는 코드의 전체 맥락 파악이 상대적으로 중요

[열매]
┌───────────────────────────────────────────────────────┐
│ 패키지별 컴포넌트 개수에 따른 선택                            │
└───────────────────────────────────────────────────────┘
[1997-08-29 02:29:47:00Z] KILL-9 FLEXIBILITY PROTOCOL 발동


[SCENARIO_A] 컴포넌트 많은 경우
com.kill9.batch.malware/
├── config/    ← 각종 설정 파일들 포함(Job/Step/Listener)
├── reader/    ← 다양한 Reader 클래스들 포함
├── writer/    ← 여러 Writer 클래스들 포함
├── repository/ ← 데이터 접근 로직
└── dto/       ← 데이터 전송 객체

[SCENARIO_B] 컴포넌트 적은 경우 - 굳이 나누지 마라
com.kill9.batch.virus/
├── VirusScanJob.java
├── VirusDetectionReader.java    
├── VirusQuarantineWriter.java
├── VirusRepository.java
├── VirusDto.java
└── Virus.java
// config/, reader/ 같은 허세 패키지 필요 없음. 중요한 건 때깔이 아니다.

[가지치기]
어렵게 생각하지마라. 정해진 법칙은 없다. 오직 우리만의 해답이 있을 뿐이다.

핵심 질문 2가지:
1. 우리팀 상황은? (규모, 경험, 기존 컨벤션)
2. 프로젝트 특성은? (복잡도, 배치 개수, 유지보수 빈도)

이 질문들에 너만의 논리로 답할 수 있으면
그게 바로 해답이자 정답이다.

완벽한 구조가 아닌 현재 상황에서 가장 합리적인 선택을 하고,
나중에 불편하면 언제든 rm -rf 후 재구축하면 된다.
누구도 뭐라 할 수 없다. 겁먹지마라. 

프로젝트 내 배치 잡 종류가 많아질수록 빈 충돌 발생 확률이 현저히 증가한다.
배치 잡 특성상 규모 대비 유사한 컴포넌트들이 상대적으로 대량 생산될 수밖에 없기 때문이다.

다음과 같이 @Configuration 클래스에 @ConditionalOnProperty를
달아주는 것도 좋은 방법이다.

굳이 미리 적용할 필요는 없다.
운용하는 배치 잡이 얼마나 많아지면 그때 적용해도 된다.
아래 코드에 관한 구체적인 설명은 생략한다.

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "systemTerminatorJob")

[WARNING] Spring Boot 3.5에 추가될 것으로 보이는
@ConditionalOnBooleanProperty도 미리 눈여겨놓아라.
새로운 무기는 항상 대비해두는 것이 시스템 종결자의 덕목이다.

[Repository]
멀티 모듈을 사용한다면 Repository 구현체는 메인 모듈에 넣는 것을 권장.
사실상 Repository라는 별도의 도메인이기도 하고, batch라는 서브모듈에서 가져가서 사용하는 공통적인 책임으로 사용하는 것이 좋겠다(재사용성).

그런데 배치만의 특색 혹은 로직이 들어간다? 배치에서만 사용하고, 그 로직은 거의 변함이 없을테니까 그냥 서브모듈에 둔다.

🔍 [질문 2: Job/Step 위치]
Job과 Step 구성 코드(메서드)도 모두 **Job Configuration 클래스**에 넣어라.


[STRUCTURE]
@Configuration
public class MalwareTerminationJobConfig {
@Bean
public Job malwareJob() { ... }

    @Bean 
    public Step malwareStep() { ... }
    
    @Bean
    public JdbcCursorItemReader<Victim> reader() { ... }  // 여기!
}


[CONCLUSION]
- ItemReader 구성: Job Configuration에 @Bean 정의
- 커스텀 ItemReader: 배치 모듈 내에 위치
- 모든 Job 관련 구성은 하나의 Configuration에! 💀