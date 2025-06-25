FROM fedora:41
WORKDIR /app
COPY target/oda-donationgoal-service /app

CMD ["./oda-donationgoal-service"]

