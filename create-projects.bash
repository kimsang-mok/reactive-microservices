mkdir microservices
cd microservices

spring init --boot-version=3.2.0 --build=gradle --java-version=21 --packaging=jar --name=product-service --package-name=com.kimsang.microservices.core.product --groupId=com.kimsang.microservices.core.product --dependencies=actuator,webflux --version=1.0.0-SNAPSHOT --type=gradle-project product-service

spring init --boot-version=3.2.0 --build=gradle --java-version=21 --packaging=jar --name=review-service --package-name=com.kimsang.microservices.core.review --groupId=com.kimsang.microservices.core.review --dependencies=actuator,webflux --version=1.0.0-SNAPSHOT --type=gradle-project review-service

spring init --boot-version=3.2.0 --build=gradle --java-version=21 --packaging=jar --name=recommendation-service --package-name=com.kimsang.microservices.core.recommendation --groupId=com.kimsang.microservices.core.recommendation --dependencies=actuator,webflux --version=1.0.0-SNAPSHOT --type=gradle-project recommendation-service

spring init --boot-version=3.2.0 --build=gradle --java-version=21 --packaging=jar --name=product-composite-service --package-name=com.kimsang.microservices.composite.product --groupId=com.kimsang.microservices.composite.product --dependencies=actuator,webflux --version=1.0.0-SNAPSHOT --type=gradle-project product-composite-service