## 1. 개요

Spring Boot 환경에서 Spring Batch Framework를 실무적으로 활용하기 위한 방법론들을 학습하여 기록한다.

## 2. 참고사항

146037ba52f8ef9fa26ee4c6487f23daf9575c88까지 monolithic 구조로 진행.
cbb0f0461abf3adb891746514a92b55b1dae4e3b이후 sub modules(MSA) 구조로 변경.

## 3. Directory Domain

1) 우선 특정 도메인(책임) 단위 또는 Job의 특성에 따라 디렉토리를 묶는것을 권한다. 다시말해 기능별로묶지말고 ‘무엇을하는지’를 기준으로 묶자는 말이다
- 보통 ItemReader / ItemWriter 등의 빈 정의도 해당 디렉토리 밖에서사용될일이 없을것이다. 안에 넣는것을권장한다

2) 여기서 공통으로사용된다거나 어디에도 넣기 애매한놈들을 / 유틸리티성 클래스들은 별도 디렉토리로빼도록하자

3) Job과 Step을 나눈다라..
   : Step을 여러 Job에서 사용한다면해당 Step을 정의한 Configuration을 별도로 빼두고 필요할때 @Import해도좋다
   (너무 복잡해지지 않는 이상은 하나로 묶는 것으로 보임)    

4) 1개의 Config 책임으로 두기보다는 Job - Step으로 책임을 분리해둔다라??
   : Job만 따로정의한 Configuration이있고 Step만 따로둔 Configuration을 별도로관리한다고이해했다 권장하진않는다 
   : 이유는 기본적으로 Step이 재사용성이 그렇게 높진않을뿐더러 이렇게나눠버리면 해당 Job이 무슨일을하는지가 덜 명확해지는경향이있는거같다 이를 나는 Job의 가독성이 낮아진다고한다.