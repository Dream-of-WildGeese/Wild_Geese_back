<div align="center">

# 온담 (Ondam)

떨어져 있어도 서로의 건강을 놓치지 않도록, 매일의 작은 기록으로 가족의 건강을 연결하는 서비스

[배포 링크](https://wild-geese-front.vercel.app/) · [Frontend 레포](https://github.com/Dream-of-WildGeese/Wild_Geese_Front) · [Backend 레포](https://github.com/Dream-of-WildGeese/Wild_Geese_back) · [API 문서](https://ondam-api.duckdns.org/swagger-ui/index.html)

멋쟁이사자처럼 at 중앙대학교 14기 · AAC Track (Wellness with AI)

</div>

---

## 소개

떨어져 사는 부모와 자녀가 하루 세 번(아침 질문·복약 체크·저녁 기록)의 가벼운 루틴으로 서로의 건강을 매일 확인하는 가족 헬스케어 서비스입니다. 기록은 실시간으로 서로에게 공유되고, AI가 하루·한 주의 기록을 읽어 코멘트와 리포트를 남깁니다.

## 핵심 기능

아침 연결 질문 · 복용 약 체크 · 저녁 건강 기록 · AI 오늘의 건강일지 · AI 주간 리포트 · 편지함 · 건강검진 알림 · 추억 보관함 · 온보딩 가이드 투어 · PWA

## 기술 스택

**Frontend**

| 분류 | 스택 |
|---|---|
| 프레임워크 | React 19, Vite |
| 스타일링 | styled-components |
| 라우팅 | react-router-dom |
| HTTP 클라이언트 | axios |
| 린트/포맷 | oxlint, prettier |
| 배포 | Vercel |
| 기타 | PWA (Web Push, Service Worker) |

**Backend**

| 분류 | 스택 |
|---|---|
| 언어/프레임워크 | Java, Spring Boot, Spring Data JPA |
| Database | MySQL, AWS RDS |
| Infra | AWS EC2, Nginx |
| Storage | AWS S3 |
| API 문서 | Swagger |

**AI 모델**

| 분류 | 모델 |
|---|---|
| LLM | gpt-4o-mini |
| STT | whisper-1 |

## 시작하기

### Frontend

```bash
git clone https://github.com/Dream-of-WildGeese/Wild_Geese_Front.git
cd Wild_Geese_Front
npm install
cp .env.example .env   # 값 채운 뒤 실행
npm run dev
```

### Backend

[Wild_Geese_back 레포](https://github.com/Dream-of-WildGeese/Wild_Geese_back)의 안내를 따라 실행합니다.

## 배포 & 데모 계정

- 배포 링크: https://wild-geese-front.vercel.app/
- 심사용 계정은 초대코드로 서로 가족 연결이 필요합니다. 자세한 계정 정보는 팀에 문의해 주세요.

## 팀 · 기러기의 꿈

| 이름 | 역할 | GitHub |
|---|---|---|
| 김다인 | PM | - |
| 윤아영 | Design | - |
| 류수민 | Frontend | [@susuu-m6](https://github.com/susuu-m6) |
| 소재희 | Frontend | [@sjhee21](https://github.com/sjhee21) |
| 권민찬 | Backend | [@tronve](https://github.com/tronve) |
| 김주연 | Backend | [@kimjuyeonstella](https://github.com/kimjuyeonstella) |

[팀 GitHub Organization](https://github.com/Dream-of-WildGeese)