# springboot-kong-keycloak

The goal is to create a [`Spring Boot`](https://docs.spring.io/spring-boot/index.html) application to manage books, called `book-service` and secure it by using [`Kong`](https://konghq.com/products/kong-gateway) API gateway and [`Keycloak`](https://www.keycloak.org) OpenID Connect Provider.

> **Note**: In [`kubernetes-minikube-environment`](https://github.com/ivangfr/kubernetes-minikube-environment/tree/master/book-service-kong-keycloak) repository, it's shown how to deploy this project in `Kubernetes` (`Minikube`)

## Proof-of-Concepts & Articles

On [ivangfr.github.io](https://ivangfr.github.io), I have compiled my Proof-of-Concepts (PoCs) and articles. You can easily search for the technology you are interested in by using the filter. Who knows, perhaps I have already implemented a PoC or written an article about what you are looking for.

## Additional Readings

- \[**Medium**\] [**Using Kong to secure a Simple Spring Boot REST API with Kong OIDC plugin and Keycloak**](https://medium.com/@ivangfr/using-kong-to-secure-a-simple-spring-boot-rest-api-with-kong-oidc-plugin-and-keycloak-c8fa8de32e6e)
- \[**Medium**\] [**Using Kong to secure a Simple Spring Boot REST API with Basic Authentication plugin**](https://medium.com/@ivangfr/using-kong-to-secure-a-simple-spring-boot-rest-api-with-basic-authentication-plugin-90f3529043f3)
- \[**Medium**\] [**Using Kong to secure a Simple Spring Boot REST API with LDAP Authentication plugin**](https://medium.com/@ivangfr/using-kong-to-secure-a-simple-spring-boot-rest-api-with-ldap-authentication-plugin-3a499e01382a)
- \[**Medium**\] [**Using Kong to configure Rate Limiting to a Simple Spring Boot REST API**](https://medium.com/@ivangfr/using-kong-to-configure-rate-limiting-to-a-simple-spring-boot-rest-api-33b1899077d)
- \[**Medium**\] [**Implementing and Securing a Simple Spring Boot REST API with Keycloak**](https://medium.com/@ivangfr/how-to-secure-a-spring-boot-app-with-keycloak-5a931ee12c5a)

## Project Diagram

![project-diagram](documentation/project-diagram.jpeg)

As we can see from the diagram, `book-service` will only be reachable through `Kong` API gateway.

In `Kong`, it's installed [`kong-oidc`](https://github.com/nokia/kong-oidc) plugin that will enable the communication between `Kong` and `Keycloak` OpenID Connect Provider.

This way, when `Kong` receives a request to `book-service`, it will validate with `Keycloak` whether it's a valid request.

Also, before redirecting to the request to the upstream service, a `Serverless Function (post-function)` will get the access token present in the `X-Userinfo` header provided by `kong-oidc` plugin, decode it, extract the `username` and `preferred_username`, and enrich the request with these two information before sending to `book-service`

## Application

- ### book-service

  `Spring Boot` REST API application to manage books. The API doesn't have any security. `book-service` uses [`MongoDB`](https://www.mongodb.com) as its storage.

  Endpoints:
  ```text
     GET /actuator/health
     GET /api/books
    POST /api/books {"isbn": "...", "title": "..."}
     GET /api/books/{isbn}
  DELETE /api/books/{isbn}
  ```

## Prerequisites

- [`Java 21`](https://www.oracle.com/java/technologies/downloads/#java21) or higher;
- A containerization tool (e.g., [`Docker`](https://www.docker.com), [`Podman`](https://podman.io), etc.)
- [`jq`](https://jqlang.github.io/jq/)

## Run application during development using Maven

- Open a terminal and navigate to the `springboot-kong-keycloak` root folder.

- Run the command below to start `mongodb` Docker container:
  ```bash
  docker run -d --name mongodb -p 27017:27017 mongo:8.0.3
  ```

- Run the command below to start `book-service`:
  ```bash
  ./mvnw clean spring-boot:run --projects book-service
  ```

- Open another terminal and call application endpoints:
  ```bash
  curl -i localhost:9080/api/books
  curl -i -X POST localhost:9080/api/books -H "Content-Type: application/json" -d '{"isbn":"123", "title":"Kong & Keycloak"}'
  curl -i localhost:9080/api/books/123
  curl -i -X DELETE localhost:9080/api/books/123
  curl -i localhost:9080/actuator/health
  ```

- To stop:
  - `book-service`, go to the terminal where it's running and press `Ctrl+C`.
  - `mongodb` Docker container, go to a terminal and run the following command:
    ```bash
    docker rm -fv mongodb
    ```

## Build application Docker Image

- In a terminal, make sure you are in the `springboot-kong-keycloak` root folder.

- Build Docker Image
  - JVM
    ```bash
    ./build-docker-images.sh
    ```
  - Native
    ```bash
    ./build-docker-images.sh native
    ```

  | Environment Variable | Description                                                       |
  |----------------------|-------------------------------------------------------------------|
  | `MONGODB_HOST`       | Specify host of the `Mongo` database to use (default `localhost`) |
  | `MONGODB_PORT`       | Specify port of the `Mongo` database to use (default `27017`)     |

## Test application Docker Image

- In a terminal, create a Docker network:
  ```bash
  docker network create springboot-kong-keycloak-net
  ```

- Run the command below to start `mongodb` Docker container:
  ```bash
  docker run -d --name mongodb -p 27017:27017 --network springboot-kong-keycloak-net mongo:8.0.3
  ```

- Run the following command to start `book-service` Docker container:
  ```bash
  docker run --rm -p 9080:9080 --name book-service -e MONGODB_HOST=mongodb --network springboot-kong-keycloak-net ivanfranchin/book-service:1.0.0
  ```

- Open another terminal and call application endpoints:
  ```bash
  curl -i localhost:9080/api/books
  curl -i -X POST localhost:9080/api/books -H "Content-Type: application/json" -d '{"isbn":"123", "title":"Kong & Keycloak"}'
  curl -i localhost:9080/api/books/123
  curl -i -X DELETE localhost:9080/api/books/123
  curl -i localhost:9080/actuator/health
  ```

- To stop:
  - `book-service`, go to the terminal where it's running and press `Ctrl+C`.
  - `mongodb` Docker container, go to a terminal and run the following command:
    ```bash
    docker rm -fv mongodb
    ```
  - Remove Docker network:
    ```bash
    docker network rm springboot-kong-keycloak-net
    ```

## Initialize Environment

- In a terminal, make use you are in the `springboot-kong-keycloak` root folder.

- Run the following script:
  ```bash
  ./init-environment.sh
  ```

> **Note**: `book-service` application is running as a Docker container. The container does not expose any port to HOST machine. So, it cannot be accessed directly, forcing the caller to use `Kong` as gateway server in order to access it.

## Configure Keycloak

- In a terminal, make sure you are in the `springboot-kong-keycloak` root folder.

- Run the following script to configure `Keycloak` for `book-service` application:
  ```bash
  ./init-keycloak.sh
  ```

  This script creates:
  - `company-services` realm;
  - `book-service` client;
  - user with _username_ `ivan.franchin` and _password_ `123`.

- The `book-service` client secret (`BOOK_SERVICE_CLIENT_SECRET`) is shown at the end of the execution. It will be used in the next step.

- You can check the configuration in `Keycloak` by accessing http://localhost:8080. The credentials are `admin/admin`.

## Configure Kong

- In a terminal, make sure you are in the `springboot-kong-keycloak` root folder.

- Create an environment variable that contains the `Client Secret` generated by `Keycloak` to `book-service` at [Configure Keycloak](#configure-keycloak) step:
  ```bash
  BOOK_SERVICE_CLIENT_SECRET=...
  ```

- Run the following script to configure `Kong` for `book-service` application:
  ```bash
  ./init-kong.sh $BOOK_SERVICE_CLIENT_SECRET
  ```
  
  This script creates:
  - service to `book-service`;
  - route to `/actuator` path;
  - route to `/api` path;
  - add `kong-oidc` plugin to route of `/api` path. It will authenticate users against `Keycloak` OpenID Connect Provider;
  - add `serverless function (post-function)` plugin to route of `/api` path. It gets the access token present in the `X-Userinfo` header provided by the `kong-oidc` plugin, decodes it, extracts the `username` and `preferred_username`, and enriches the request with this information before sending it to `book-service`.

## Testing

- Try to call the public `GET /actuator/health` endpoint:
  ```bash
  curl -i localhost:8000/actuator/health -H 'Host: book-service'
  ```
  It should return:
  ```text
  HTTP/1.1 200
  {"status":"UP"}
  ```

- Try to call the private `GET /api/books` endpoint without access token:
  ```bash
  curl -i localhost:8000/api/books -H 'Host: book-service'
  ```
  It should return:
  ```text
  HTTP/1.1 401 Unauthorized
  no Authorization header found
  ```

- Get `ivan.franchin` access token:
  ```bash
  ACCESS_TOKEN=$(./get-access-token.sh $BOOK_SERVICE_CLIENT_SECRET) && echo $ACCESS_TOKEN
  ```
  > **Note**: In `jwt.io`, you can decode and verify the `JWT` access token

- Call again the private `GET /api/books` endpoint using the access token:
  ```bash
  curl -i localhost:8000/api/books \
    -H 'Host: book-service' \
    -H "Authorization: Bearer $ACCESS_TOKEN"
  ```
  It should return:
  ```text
  HTTP/1.1 200
  []
  ```

- You can try other endpoints using access token:

  Create book
  ```bash
  curl -i -X POST localhost:8000/api/books \
    -H 'Host: book-service' \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" -d '{"isbn": "123", "title": "Kong & Keycloak"}'
  ```
  
  Get book
  ```bash
  curl -i localhost:8000/api/books/123 \
    -H 'Host: book-service' \
    -H "Authorization: Bearer $ACCESS_TOKEN"
  ```
  
  Delete book 
  ```bash
  curl -i -X DELETE localhost:8000/api/books/123 \
    -H 'Host: book-service' \
    -H "Authorization: Bearer $ACCESS_TOKEN"
  ```

## Useful Links & Commands

- **MongoDB**

  List books
  ```bash
  docker exec -it mongodb mongosh bookdb
  db.books.find()
  ```
  > Type `exit` to get out of MongoDB shell

## Shutdown

In a terminal and, inside the `springboot-kong-keycloak` root folder, run the following script:
```bash
./shutdown-environment.sh
```

## Cleanup

To remove the Docker image created by this project, in a terminal and, inside the `springboot-kong-keycloak` root folder, run the script below:
```bash
./remove-docker-images.sh
```

## References

- https://www.jerney.io/secure-apis-kong-keycloak-1/
- https://github.com/d4rkstar/kong-konga-keycloak

## Issues

- Unable to upgrade to `kong` version `3.x` because `BasePlugin` class was deprecated in `kong` version `2.4.x` and removed in version `3.0.x` [link](https://support.konghq.com/support/s/article/Custom-plugins-not-loading-with-no-LuaRocks-module-found-for-kong-plugins-base-plugin). Now, `kong-oidc` needs to support `kong` version `3.x` [issue](https://github.com/nokia/kong-oidc/issues/213);

- When upgrading `postgres` to a version above `13.x` (using current kong version), there is an error while running `kong`
  ```text
  [error] 1#0: init_by_lua error: /usr/local/share/lua/5.1/pgmoon/init.lua:273: module 'openssl.rand' not found:No LuaRocks module found for openssl.rand
  	no field package.preload['openssl.rand']
  	no file './openssl/rand.lua'
  	no file './openssl/rand/init.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.ljbc'
  	no file '/usr/local/openresty/site/lualib/openssl/rand/init.ljbc'
  	no file '/usr/local/openresty/lualib/openssl/rand.ljbc'
  	no file '/usr/local/openresty/lualib/openssl/rand/init.ljbc'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand/init.lua'
  	no file '/usr/local/openresty/lualib/openssl/rand.lua'
  	no file '/usr/local/openresty/lualib/openssl/rand/init.lua'
  	no file '/usr/local/openresty/luajit/share/luajit-2.1.0-beta3/openssl/rand.lua'
  	no file '/usr/local/share/lua/5.1/openssl/rand.lua'
  	no file '/usr/local/share/lua/5.1/openssl/rand/init.lua'
  	no file '/usr/local/openresty/luajit/share/lua/5.1/openssl/rand.lua'
  	no file '/usr/local/openresty/luajit/share/lua/5.1/openssl/rand/init.lua'
  	no file '/home/kong/.luarocks/share/lua/5.1/openssl/rand.lua'
  	no file '/home/kong/.luarocks/share/lua/5.1/openssl/rand/init.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.so'
  	no file '/usr/local/openresty/lualib/openssl/rand.so'
  	no file './openssl/rand.so'
  	no file '/usr/local/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/openresty/luajit/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/lib/lua/5.1/loadall.so'
  	no file '/home/kong/.luarocks/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/openresty/site/lualib/openssl.so'
  	no file '/usr/local/openresty/lualib/openssl.so'
  	no file './openssl.so'
  	no file '/usr/local/lib/lua/5.1/openssl.so'
  	no file '/usr/local/openresty/luajit/lib/lua/5.1/openssl.so'
  	no file '/usr/local/lib/lua/5.1/loadall.so'
  	no file '/home/kong/.luarocks/lib/lua/5.1/openssl.so'
  stack traceback:
  	[C]: in function 'require'
  	/usr/local/share/lua/5.1/pgmoon/init.lua:273: in function 'auth'
  	/usr/local/share/lua/5.1/pgmoon/init.lua:213: in function 'connect'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:216: in function 'connect'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:516: in function 'query'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:284: in function 'init'
  	/usr/local/share/lua/5.1/kong/db/init.lua:141: in function 'init_connector'
  	/usr/local/share/lua/5.1/kong/init.lua:503: in function 'init'
  	init_by_lua:3: in main chunk
  nginx: [error] init_by_lua error: /usr/local/share/lua/5.1/pgmoon/init.lua:273: module 'openssl.rand' not found:No LuaRocks module found for openssl.rand
  	no field package.preload['openssl.rand']
  	no file './openssl/rand.lua'
  	no file './openssl/rand/init.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.ljbc'
  	no file '/usr/local/openresty/site/lualib/openssl/rand/init.ljbc'
  	no file '/usr/local/openresty/lualib/openssl/rand.ljbc'
  	no file '/usr/local/openresty/lualib/openssl/rand/init.ljbc'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand/init.lua'
  	no file '/usr/local/openresty/lualib/openssl/rand.lua'
  	no file '/usr/local/openresty/lualib/openssl/rand/init.lua'
  	no file '/usr/local/openresty/luajit/share/luajit-2.1.0-beta3/openssl/rand.lua'
  	no file '/usr/local/share/lua/5.1/openssl/rand.lua'
  	no file '/usr/local/share/lua/5.1/openssl/rand/init.lua'
  	no file '/usr/local/openresty/luajit/share/lua/5.1/openssl/rand.lua'
  	no file '/usr/local/openresty/luajit/share/lua/5.1/openssl/rand/init.lua'
  	no file '/home/kong/.luarocks/share/lua/5.1/openssl/rand.lua'
  	no file '/home/kong/.luarocks/share/lua/5.1/openssl/rand/init.lua'
  	no file '/usr/local/openresty/site/lualib/openssl/rand.so'
  	no file '/usr/local/openresty/lualib/openssl/rand.so'
  	no file './openssl/rand.so'
  	no file '/usr/local/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/openresty/luajit/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/lib/lua/5.1/loadall.so'
  	no file '/home/kong/.luarocks/lib/lua/5.1/openssl/rand.so'
  	no file '/usr/local/openresty/site/lualib/openssl.so'
  	no file '/usr/local/openresty/lualib/openssl.so'
  	no file './openssl.so'
  	no file '/usr/local/lib/lua/5.1/openssl.so'
  	no file '/usr/local/openresty/luajit/lib/lua/5.1/openssl.so'
  	no file '/usr/local/lib/lua/5.1/loadall.so'
  	no file '/home/kong/.luarocks/lib/lua/5.1/openssl.so'
  stack traceback:
  	[C]: in function 'require'
  	/usr/local/share/lua/5.1/pgmoon/init.lua:273: in function 'auth'
  	/usr/local/share/lua/5.1/pgmoon/init.lua:213: in function 'connect'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:216: in function 'connect'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:516: in function 'query'
  	.../share/lua/5.1/kong/db/strategies/postgres/connector.lua:284: in function 'init'
  	/usr/local/share/lua/5.1/kong/db/init.lua:141: in function 'init_connector'
  	/usr/local/share/lua/5.1/kong/init.lua:503: in function 'init'
  	init_by_lua:3: in main chunk
  ```