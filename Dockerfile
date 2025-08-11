FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/databus-derive-maven-plugin-1.0-SNAPSHOT.jar target/

COPY src/main/python src/main/python

COPY ntfiles ntfiles

RUN pip install --no-cache-dir -r src/main/python/requirements.txt

ENTRYPOINT ["python", "src/main/python/run_ntparser.py"]
