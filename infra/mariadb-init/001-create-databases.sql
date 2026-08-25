-- MARIADB_DATABASE 환경변수는 DB 하나만 만들어주므로, 서비스별로 DB가 늘어날 때마다
-- 여기에 CREATE DATABASE를 추가한다. 이 폴더는 컨테이너 최초 생성 시(빈 볼륨)에만 실행되므로
-- 이미 떠 있는 로컬 컨테이너에는 수동으로 CREATE DATABASE를 실행해야 한다.
CREATE DATABASE IF NOT EXISTS gyubot_auth;
CREATE DATABASE IF NOT EXISTS gyubot_user;
