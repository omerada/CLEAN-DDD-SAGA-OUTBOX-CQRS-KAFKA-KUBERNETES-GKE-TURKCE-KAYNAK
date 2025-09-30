# ☁️ Google Cloud GKE Production Deployment - Bölüm 11

## 📋 Özet

Bu final bölümde mikroservisleri Google Kubernetes Engine (GKE) üzerinde production-ready olarak deploy edeceğiz. CI/CD pipelines, monitoring, security, auto-scaling ve disaster recovery'yi öğreneceksiniz.

## 🎯 Öğrenme Hedefleri

Bu bölüm sonunda şunları yapabileceksiniz:

- ✅ **GKE Cluster Management** - Production cluster kurulumu ve yapılandırması
- ✅ **CI/CD Pipelines** - GitHub Actions ile automated deployment
- ✅ **Production Security** - RBAC, Network Policies, Pod Security Standards
- ✅ **Monitoring & Observability** - Prometheus, Grafana, Logging stack
- ✅ **Auto-scaling** - HPA, VPA, Cluster Autoscaler
- ✅ **Disaster Recovery** - Backup strategies, multi-region deployment
- ✅ **Performance Optimization** - Resource management, cost optimization
- ✅ **Service Mesh** - Istio implementation (optional advanced)

## 📋 Ön Koşullar

- ✅ Kubernetes Fundamentals tamamlanmış (Bölüm 10)
- ✅ Google Cloud Platform account
- ✅ `gcloud` CLI installed and configured
- ✅ Docker Hub/Container Registry access
- ✅ GitHub repository for CI/CD

---

## ☁️ GKE Cluster Setup

### Google Cloud Project Setup

```bash
# 1. Create new project
gcloud projects create microservices-prod-2024 \
    --name="Microservices Production"

# 2. Set project
gcloud config set project microservices-prod-2024

# 3. Enable required APIs
gcloud services enable container.googleapis.com
gcloud services enable containerregistry.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable monitoring.googleapis.com
gcloud services enable logging.googleapis.com

# 4. Create service account for CI/CD
gcloud iam service-accounts create gke-deployer \
    --display-name="GKE Deployer Service Account"

# 5. Grant permissions
gcloud projects add-iam-policy-binding microservices-prod-2024 \
    --member="serviceAccount:gke-deployer@microservices-prod-2024.iam.gserviceaccount.com" \
    --role="roles/container.admin"

gcloud projects add-iam-policy-binding microservices-prod-2024 \
    --member="serviceAccount:gke-deployer@microservices-prod-2024.iam.gserviceaccount.com" \
    --role="roles/storage.admin"

# 6. Create and download key
gcloud iam service-accounts keys create gke-deployer-key.json \
    --iam-account=gke-deployer@microservices-prod-2024.iam.gserviceaccount.com
```

### Production GKE Cluster Configuration

```bash
# Create production cluster
gcloud container clusters create microservices-production \
    --zone=us-central1-a \
    --machine-type=e2-standard-4 \
    --num-nodes=3 \
    --min-nodes=2 \
    --max-nodes=10 \
    --enable-autoscaling \
    --enable-autorepair \
    --enable-autoupgrade \
    --enable-network-policy \
    --enable-ip-alias \
    --enable-shielded-nodes \
    --shielded-secure-boot \
    --shielded-integrity-monitoring \
    --workload-pool=microservices-prod-2024.svc.id.goog \
    --cluster-version=1.28 \
    --addons=HorizontalPodAutoscaling,HttpLoadBalancing,NetworkPolicy \
    --enable-stackdriver-kubernetes \
    --disk-size=50GB \
    --disk-type=pd-ssd \
    --node-labels=environment=production \
    --node-taints=dedicated=production:NoSchedule

# Create node pools for different workloads
# 1. General workload pool (default above)

# 2. High-memory pool for databases
gcloud container node-pools create high-memory-pool \
    --cluster=microservices-production \
    --zone=us-central1-a \
    --machine-type=e2-highmem-2 \
    --num-nodes=2 \
    --min-nodes=1 \
    --max-nodes=5 \
    --enable-autoscaling \
    --node-labels=workload=database,environment=production \
    --node-taints=dedicated=database:NoSchedule

# 3. CPU-optimized pool for compute-intensive tasks
gcloud container node-pools create cpu-optimized-pool \
    --cluster=microservices-production \
    --zone=us-central1-a \
    --machine-type=e2-highcpu-4 \
    --num-nodes=1 \
    --min-nodes=0 \
    --max-nodes=5 \
    --enable-autoscaling \
    --node-labels=workload=compute,environment=production \
    --node-taints=dedicated=compute:NoSchedule

# Get credentials
gcloud container clusters get-credentials microservices-production \
    --zone=us-central1-a

# Verify cluster
kubectl cluster-info
kubectl get nodes -o wide
```

### Cluster Architecture

```ascii
┌─────────────────────────────────────────────────────────────────────┐
│                        GKE PRODUCTION CLUSTER                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  🌐 GOOGLE CLOUD LOAD BALANCER                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Internet Traffic → Cloud Load Balancer → Ingress Gateway  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  🔧 NODE POOLS                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  🖥️ General Purpose Pool (e2-standard-4)                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │   │
│  │  │   Node 1    │  │   Node 2    │  │   Node 3    │       │   │
│  │  │             │  │             │  │             │       │   │
│  │  │ App Pods    │  │ App Pods    │  │ App Pods    │       │   │
│  │  │ Services    │  │ Services    │  │ Services    │       │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │   │
│  │                                                             │   │
│  │  💾 High-Memory Pool (e2-highmem-2)                       │   │
│  │  ┌─────────────┐  ┌─────────────┐                        │   │
│  │  │   Node 4    │  │   Node 5    │                        │   │
│  │  │             │  │             │                        │   │
│  │  │ PostgreSQL  │  │  Kafka      │                        │   │
│  │  │ Redis       │  │  Elasticsearch                       │   │
│  │  └─────────────┘  └─────────────┘                        │   │
│  │                                                             │   │
│  │  ⚡ CPU-Optimized Pool (e2-highcpu-4)                    │   │
│  │  ┌─────────────┐                                          │   │
│  │  │   Node 6    │                                          │   │
│  │  │             │                                          │   │
│  │  │ Analytics   │                                          │   │
│  │  │ ML Workloads│                                          │   │
│  │  └─────────────┘                                          │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  📊 MONITORING & OBSERVABILITY                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  Cloud Monitoring | Cloud Logging | Prometheus | Grafana  │   │
│  │  Jaeger Tracing  | Alertmanager  | PagerDuty Integration  │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  🔐 SECURITY                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                             │   │
│  │  Workload Identity | Binary Authorization | Pod Security   │   │
│  │  Network Policies  | Private Cluster     | VPC Native     │   │
│  │  Shielded Nodes   | RBAC              | Secret Manager  │   │
│  │                                                             │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🐳 Container Registry & Image Management

### Google Container Registry Setup

```bash
# Configure Docker for GCR
gcloud auth configure-docker

# Tag and push images
# Assuming you have built images locally

# Order Service
docker tag order-service:latest gcr.io/microservices-prod-2024/order-service:v1.0.0
docker tag order-service:latest gcr.io/microservices-prod-2024/order-service:latest
docker push gcr.io/microservices-prod-2024/order-service:v1.0.0
docker push gcr.io/microservices-prod-2024/order-service:latest

# Inventory Service
docker tag inventory-service:latest gcr.io/microservices-prod-2024/inventory-service:v1.0.0
docker push gcr.io/microservices-prod-2024/inventory-service:v1.0.0

# Payment Service
docker tag payment-service:latest gcr.io/microservices-prod-2024/payment-service:v1.0.0
docker push gcr.io/microservices-prod-2024/payment-service:v1.0.0

# Verify images
gcloud container images list --repository=gcr.io/microservices-prod-2024
```

### Production Dockerfile Optimization

```dockerfile
# order-service/Dockerfile.prod
FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests -Dspring.profiles.active=production

FROM amazoncorretto:21-alpine AS runtime
LABEL org.opencontainers.image.title="Order Service"
LABEL org.opencontainers.image.description="Production Order Service for Microservices Architecture"
LABEL org.opencontainers.image.vendor="Your Company"
LABEL org.opencontainers.image.version="1.0.0"

# Security: Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Install security updates
RUN apk update && apk upgrade && apk add --no-cache curl

WORKDIR /app

# Copy JAR with specific ownership
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 🔐 Production Security Configuration

### RBAC Setup

```yaml
# rbac.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: microservices-prod
  labels:
    environment: production
    project: microservices

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: microservices-sa
  namespace: microservices-prod

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: microservices-role
rules:
  - apiGroups: [""]
    resources: ["pods", "services", "endpoints", "configmaps", "secrets"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]
    resources: ["deployments", "replicasets"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["metrics.k8s.io"]
    resources: ["pods", "nodes"]
    verbs: ["get", "list"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: microservices-role-binding
subjects:
  - kind: ServiceAccount
    name: microservices-sa
    namespace: microservices-prod
roleRef:
  kind: ClusterRole
  name: microservices-role
  apiGroup: rbac.authorization.k8s.io

---
# Deployment service account
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: microservices-prod
  name: deployment-manager
rules:
  - apiGroups: ["", "apps", "extensions", "networking.k8s.io"]
    resources: ["*"]
    verbs: ["*"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: deployment-manager-binding
  namespace: microservices-prod
subjects:
  - kind: ServiceAccount
    name: microservices-sa
    namespace: microservices-prod
roleRef:
  kind: Role
  name: deployment-manager
  apiGroup: rbac.authorization.k8s.io
```

### Pod Security Standards

```yaml
# pod-security.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: microservices-prod
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted

---
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: microservices-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - "configMap"
    - "emptyDir"
    - "projected"
    - "secret"
    - "downwardAPI"
    - "persistentVolumeClaim"
  runAsUser:
    rule: "MustRunAsNonRoot"
  seLinux:
    rule: "RunAsAny"
  fsGroup:
    rule: "RunAsAny"
  readOnlyRootFilesystem: false
```

### Network Policies

```yaml
# network-policies.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all
  namespace: microservices-prod
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-ingress-traffic
  namespace: microservices-prod
spec:
  podSelector:
    matchLabels:
      tier: web
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-internal-communication
  namespace: microservices-prod
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: microservices-prod
  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              name: microservices-prod
    - to: []
      ports:
        - protocol: TCP
          port: 53
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 443

---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-database-access
  namespace: microservices-prod
spec:
  podSelector:
    matchLabels:
      app: postgres
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              tier: backend
      ports:
        - protocol: TCP
          port: 5432
```

---

## 🏗️ Production Deployment Manifests

### PostgreSQL Production Setup

```yaml
# postgres-production.yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-credentials
  namespace: microservices-prod
type: Opaque
data:
  postgres-password: <base64-encoded-password>
  order-db-password: <base64-encoded-password>
  inventory-db-password: <base64-encoded-password>
  payment-db-password: <base64-encoded-password>

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: microservices-prod
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: ssd-retain
  resources:
    requests:
      storage: 100Gi

---
apiVersion: v1
kind: StorageClass
metadata:
  name: ssd-retain
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
  fstype: ext4
reclaimPolicy: Retain
allowVolumeExpansion: true

---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: microservices-prod
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
        tier: database
    spec:
      serviceAccountName: microservices-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 999
        fsGroup: 999
      nodeSelector:
        workload: database
      tolerations:
        - key: dedicated
          operator: Equal
          value: database
          effect: NoSchedule
      containers:
        - name: postgres
          image: postgres:15-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: "postgres"
            - name: POSTGRES_USER
              value: "postgres"
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: postgres-password
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          volumeMounts:
            - name: postgres-storage
              mountPath: /var/lib/postgresql/data
            - name: postgres-config
              mountPath: /etc/postgresql/postgresql.conf
              subPath: postgresql.conf
            - name: postgres-init
              mountPath: /docker-entrypoint-initdb.d
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "1"
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: false
            capabilities:
              drop:
                - ALL
          livenessProbe:
            exec:
              command:
                - /bin/sh
                - -c
                - pg_isready -U postgres
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            exec:
              command:
                - /bin/sh
                - -c
                - pg_isready -U postgres
            initialDelaySeconds: 5
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
      volumes:
        - name: postgres-storage
          persistentVolumeClaim:
            claimName: postgres-pvc
        - name: postgres-config
          configMap:
            name: postgres-config
        - name: postgres-init
          configMap:
            name: postgres-init-scripts

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: microservices-prod
  labels:
    app: postgres
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
  clusterIP: None # Headless service for StatefulSet

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  namespace: microservices-prod
data:
  postgresql.conf: |
    # PostgreSQL Production Configuration
    shared_preload_libraries = 'pg_stat_statements'

    # Connection Settings
    max_connections = 200
    shared_buffers = 512MB

    # Write Ahead Logging
    wal_level = replica
    max_wal_size = 2GB
    min_wal_size = 80MB
    checkpoint_completion_target = 0.9

    # Query Tuning
    random_page_cost = 1.1
    effective_cache_size = 1GB
    work_mem = 4MB
    maintenance_work_mem = 64MB

    # Logging
    log_destination = 'stderr'
    logging_collector = on
    log_min_duration_statement = 1000
    log_statement = 'all'
    log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
```

### Order Service Production Deployment

```yaml
# order-service-production.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-service-config
  namespace: microservices-prod
data:
  application-production.yml: |
    server:
      port: 8080
      shutdown: graceful

    spring:
      application:
        name: order-service
      profiles:
        active: production
      
      datasource:
        url: jdbc:postgresql://postgres-service:5432/order_db
        username: order_user
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          idle-timeout: 300000
          max-lifetime: 1800000
          connection-timeout: 30000
          validation-timeout: 5000
          leak-detection-threshold: 60000
      
      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect
            format_sql: false
            jdbc:
              batch_size: 25
              order_inserts: true
              order_updates: true
      
      kafka:
        bootstrap-servers: kafka-service:9092
        producer:
          acks: all
          retries: 3
          enable-idempotence: true
          compression-type: snappy
          batch-size: 16384
          linger-ms: 5
          buffer-memory: 33554432
        consumer:
          group-id: order-service-group
          auto-offset-reset: earliest
          enable-auto-commit: false
          max-poll-records: 500
          max-poll-interval-ms: 300000

    # Resilience4j Circuit Breaker
    resilience4j:
      circuitbreaker:
        instances:
          inventory-service:
            registerHealthIndicator: true
            slidingWindowSize: 10
            minimumNumberOfCalls: 5
            permittedNumberOfCallsInHalfOpenState: 3
            automaticTransitionFromOpenToHalfOpenEnabled: true
            waitDurationInOpenState: 5s
            failureRateThreshold: 50
            eventConsumerBufferSize: 10
          payment-service:
            registerHealthIndicator: true
            slidingWindowSize: 10
            minimumNumberOfCalls: 5
            failureRateThreshold: 50
      
      retry:
        instances:
          order-processing:
            maxAttempts: 3
            waitDuration: 1s
            exponentialBackoffMultiplier: 2
            retryExceptions:
              - java.net.ConnectException
              - java.util.concurrent.TimeoutException

    # Monitoring & Observability
    management:
      endpoints:
        web:
          exposure:
            include: health,metrics,prometheus,info,env
      endpoint:
        health:
          show-details: always
          probes:
            enabled: true
      health:
        livenessstate:
          enabled: true
        readinessstate:
          enabled: true
        diskspace:
          enabled: true
          threshold: 10GB
      metrics:
        export:
          prometheus:
            enabled: true
        distribution:
          percentiles-histogram:
            "[http.server.requests]": true
          percentiles:
            "[http.server.requests]": 0.5, 0.95, 0.99

    # Logging
    logging:
      level:
        com.example.order: INFO
        org.springframework.kafka: WARN
        org.hibernate.SQL: WARN
        org.springframework.web.filter.CommonsRequestLoggingFilter: DEBUG
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"

    # Custom Application Properties
    order:
      processing:
        timeout: 30s
        max-retry-attempts: 3
        retry-delay: 1s
      
      saga:
        timeout: 2m
        compensation-enabled: true
      
      outbox:
        cleanup-interval: 5m
        max-age: 24h

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: microservices-prod
  labels:
    app: order-service
    version: v1.0.0
    tier: backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: order-service
      version: v1.0.0
  template:
    metadata:
      labels:
        app: order-service
        version: v1.0.0
        tier: backend
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      serviceAccountName: microservices-sa
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        fsGroup: 1001
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - order-service
                topologyKey: kubernetes.io/hostname
      initContainers:
        - name: wait-for-postgres
          image: postgres:15-alpine
          command:
            - /bin/sh
            - -c
            - |
              until pg_isready -h postgres-service -p 5432; do
                echo "Waiting for PostgreSQL..."
                sleep 2
              done
              echo "PostgreSQL is ready!"
          securityContext:
            runAsNonRoot: true
            runAsUser: 999
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
        - name: wait-for-kafka
          image: confluentinc/cp-kafka:7.4.0
          command:
            - /bin/sh
            - -c
            - |
              until kafka-broker-api-versions --bootstrap-server kafka-service:9092; do
                echo "Waiting for Kafka..."
                sleep 2
              done
              echo "Kafka is ready!"
          securityContext:
            runAsNonRoot: true
            runAsUser: 1001
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
      containers:
        - name: order-service
          image: gcr.io/microservices-prod-2024/order-service:v1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
              protocol: TCP
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: SPRING_CONFIG_LOCATION
              value: "classpath:/application.yml,/config/application-production.yml"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-credentials
                  key: order-db-password
            - name: JAVA_OPTS
              value: "-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=production"
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: POD_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.podIP
          volumeMounts:
            - name: config-volume
              mountPath: /config
            - name: tmp-volume
              mountPath: /tmp
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
              ephemeral-storage: "1Gi"
            limits:
              memory: "2Gi"
              cpu: "1"
              ephemeral-storage: "2Gi"
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 30
      volumes:
        - name: config-volume
          configMap:
            name: order-service-config
        - name: tmp-volume
          emptyDir: {}
      terminationGracePeriodSeconds: 30
      dnsPolicy: ClusterFirst
      restartPolicy: Always

---
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: microservices-prod
  labels:
    app: order-service
  annotations:
    cloud.google.com/load-balancer-type: "Internal"
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
      name: http
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: microservices-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 2
          periodSeconds: 60
```

---

## 🚀 CI/CD Pipeline with GitHub Actions

### GitHub Actions Workflow

```yaml
# .github/workflows/deploy-production.yml
name: Deploy to GKE Production

on:
  push:
    branches:
      - main
    paths:
      - "order-service/**"
      - "inventory-service/**"
      - "payment-service/**"
      - "k8s/**"
  pull_request:
    branches:
      - main

env:
  PROJECT_ID: microservices-prod-2024
  GKE_CLUSTER: microservices-production
  GKE_ZONE: us-central1-a

jobs:
  setup-build-publish-deploy:
    name: Setup, Build, Publish, and Deploy
    runs-on: ubuntu-latest
    environment: production

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "corretto"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      # Setup gcloud CLI
      - uses: google-github-actions/setup-gcloud@v1
        with:
          service_account_key: ${{ secrets.GKE_SA_KEY }}
          project_id: ${{ secrets.GKE_PROJECT }}

      # Configure Docker to use the gcloud command-line tool as a credential helper
      - run: |
          gcloud --quiet auth configure-docker

      # Get the GKE credentials so we can deploy to the cluster
      - run: |
          gcloud container clusters get-credentials "$GKE_CLUSTER" --zone "$GKE_ZONE"

      # Build and test
      - name: Run tests
        run: |
          cd order-service && mvn clean test
          cd ../inventory-service && mvn clean test
          cd ../payment-service && mvn clean test

      # Build Docker images
      - name: Build Order Service
        run: |
          cd order-service
          mvn clean package -DskipTests
          docker build -f Dockerfile.prod -t gcr.io/$PROJECT_ID/order-service:$GITHUB_SHA .
          docker tag gcr.io/$PROJECT_ID/order-service:$GITHUB_SHA gcr.io/$PROJECT_ID/order-service:latest

      - name: Build Inventory Service
        run: |
          cd inventory-service
          mvn clean package -DskipTests
          docker build -f Dockerfile.prod -t gcr.io/$PROJECT_ID/inventory-service:$GITHUB_SHA .
          docker tag gcr.io/$PROJECT_ID/inventory-service:$GITHUB_SHA gcr.io/$PROJECT_ID/inventory-service:latest

      - name: Build Payment Service
        run: |
          cd payment-service
          mvn clean package -DskipTests
          docker build -f Dockerfile.prod -t gcr.io/$PROJECT_ID/payment-service:$GITHUB_SHA .
          docker tag gcr.io/$PROJECT_ID/payment-service:$GITHUB_SHA gcr.io/$PROJECT_ID/payment-service:latest

      # Push images to Container Registry
      - name: Publish to Container Registry
        run: |
          docker push gcr.io/$PROJECT_ID/order-service:$GITHUB_SHA
          docker push gcr.io/$PROJECT_ID/order-service:latest
          docker push gcr.io/$PROJECT_ID/inventory-service:$GITHUB_SHA
          docker push gcr.io/$PROJECT_ID/inventory-service:latest
          docker push gcr.io/$PROJECT_ID/payment-service:$GITHUB_SHA
          docker push gcr.io/$PROJECT_ID/payment-service:latest

      # Deploy to GKE
      - name: Deploy to GKE
        run: |
          # Apply configurations
          kubectl apply -f k8s/production/namespace.yaml
          kubectl apply -f k8s/production/rbac.yaml
          kubectl apply -f k8s/production/network-policies.yaml

          # Apply secrets (assuming they exist)
          kubectl apply -f k8s/production/secrets.yaml

          # Deploy infrastructure
          kubectl apply -f k8s/production/postgres-production.yaml
          kubectl apply -f k8s/production/kafka-production.yaml

          # Wait for infrastructure
          kubectl wait --for=condition=ready pod -l app=postgres -n microservices-prod --timeout=300s
          kubectl wait --for=condition=ready pod -l app=kafka -n microservices-prod --timeout=300s

          # Update image tags and deploy services
          sed -i "s|gcr.io/$PROJECT_ID/order-service:v1.0.0|gcr.io/$PROJECT_ID/order-service:$GITHUB_SHA|g" k8s/production/order-service-production.yaml
          sed -i "s|gcr.io/$PROJECT_ID/inventory-service:v1.0.0|gcr.io/$PROJECT_ID/inventory-service:$GITHUB_SHA|g" k8s/production/inventory-service-production.yaml
          sed -i "s|gcr.io/$PROJECT_ID/payment-service:v1.0.0|gcr.io/$PROJECT_ID/payment-service:$GITHUB_SHA|g" k8s/production/payment-service-production.yaml

          kubectl apply -f k8s/production/

          # Verify deployment
          kubectl rollout status deployment/order-service -n microservices-prod
          kubectl rollout status deployment/inventory-service -n microservices-prod
          kubectl rollout status deployment/payment-service -n microservices-prod

          # Get service info
          kubectl get services -n microservices-prod

      # Smoke tests
      - name: Run smoke tests
        run: |
          # Wait for services to be ready
          sleep 60

          # Get load balancer IP
          INGRESS_IP=$(kubectl get ingress microservices-ingress -n microservices-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

          # Run basic health checks
          curl -f http://$INGRESS_IP/orders/actuator/health || exit 1
          curl -f http://$INGRESS_IP/inventory/actuator/health || exit 1
          curl -f http://$INGRESS_IP/payments/actuator/health || exit 1

          echo "All services are healthy!"

      # Notify on failure
      - name: Notify failure
        if: failure()
        uses: 8398a7/action-slack@v3
        with:
          status: failure
          channel: "#deployments"
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Deployment Scripts

```bash
#!/bin/bash
# scripts/deploy-production.sh

set -e

PROJECT_ID="microservices-prod-2024"
CLUSTER_NAME="microservices-production"
ZONE="us-central1-a"
NAMESPACE="microservices-prod"

echo "🚀 Starting production deployment..."

# Authenticate with Google Cloud
echo "🔐 Authenticating with Google Cloud..."
gcloud auth activate-service-account --key-file=gke-deployer-key.json
gcloud config set project $PROJECT_ID

# Get cluster credentials
echo "📡 Getting cluster credentials..."
gcloud container clusters get-credentials $CLUSTER_NAME --zone $ZONE

# Apply configurations
echo "⚙️ Applying configurations..."
kubectl apply -f k8s/production/namespace.yaml
kubectl apply -f k8s/production/rbac.yaml
kubectl apply -f k8s/production/network-policies.yaml

# Check if secrets exist, if not create them
echo "🔒 Checking secrets..."
if ! kubectl get secret postgres-credentials -n $NAMESPACE >/dev/null 2>&1; then
    echo "Creating postgres credentials secret..."
    kubectl create secret generic postgres-credentials \
        --from-literal=postgres-password=$(openssl rand -base64 32) \
        --from-literal=order-db-password=$(openssl rand -base64 32) \
        --from-literal=inventory-db-password=$(openssl rand -base64 32) \
        --from-literal=payment-db-password=$(openssl rand -base64 32) \
        -n $NAMESPACE
fi

# Deploy infrastructure
echo "🏗️ Deploying infrastructure..."
kubectl apply -f k8s/production/postgres-production.yaml
kubectl apply -f k8s/production/kafka-production.yaml

# Wait for infrastructure
echo "⏳ Waiting for infrastructure to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=300s

# Deploy services
echo "🚀 Deploying microservices..."
kubectl apply -f k8s/production/order-service-production.yaml
kubectl apply -f k8s/production/inventory-service-production.yaml
kubectl apply -f k8s/production/payment-service-production.yaml

# Deploy ingress
kubectl apply -f k8s/production/ingress-production.yaml

# Wait for deployments
echo "⏳ Waiting for deployments to be ready..."
kubectl rollout status deployment/order-service -n $NAMESPACE --timeout=300s
kubectl rollout status deployment/inventory-service -n $NAMESPACE --timeout=300s
kubectl rollout status deployment/payment-service -n $NAMESPACE --timeout=300s

# Get status
echo "📊 Deployment status:"
kubectl get all -n $NAMESPACE

# Get ingress IP
echo "🌐 Getting ingress IP..."
INGRESS_IP=$(kubectl get ingress microservices-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "Ingress IP: $INGRESS_IP"

# Run health checks
echo "🏥 Running health checks..."
sleep 60

curl -f http://$INGRESS_IP/orders/actuator/health
curl -f http://$INGRESS_IP/inventory/actuator/health
curl -f http://$INGRESS_IP/payments/actuator/health

echo "✅ Production deployment completed successfully!"
```

---

## 📊 Monitoring & Observability

### Prometheus & Grafana Setup

```yaml
# monitoring/prometheus.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
        - name: prometheus
          image: prom/prometheus:v2.45.0
          ports:
            - containerPort: 9090
          volumeMounts:
            - name: config-volume
              mountPath: /etc/prometheus
            - name: storage-volume
              mountPath: /prometheus
          command:
            - /bin/prometheus
            - --config.file=/etc/prometheus/prometheus.yml
            - --storage.tsdb.path=/prometheus
            - --web.console.libraries=/usr/share/prometheus/console_libraries
            - --web.console.templates=/usr/share/prometheus/consoles
            - --storage.tsdb.retention.time=15d
            - --web.enable-lifecycle
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "1"
      volumes:
        - name: config-volume
          configMap:
            name: prometheus-config
        - name: storage-volume
          persistentVolumeClaim:
            claimName: prometheus-pvc

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
    - job_name: 'kubernetes-pods'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
      - action: labelmap
        regex: __meta_kubernetes_pod_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        action: replace
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_pod_name]
        action: replace
        target_label: kubernetes_pod_name

    - job_name: 'kubernetes-nodes'
      kubernetes_sd_configs:
      - role: node
      relabel_configs:
      - action: labelmap
        regex: __meta_kubernetes_node_label_(.+)

---
apiVersion: v1
kind: Service
metadata:
  name: prometheus-service
  namespace: monitoring
spec:
  selector:
    app: prometheus
  ports:
    - port: 9090
      targetPort: 9090
  type: LoadBalancer
```

Bu production deployment rehberi ile:

- ✅ **Enterprise-grade security** ile GKE cluster kurabilir
- ✅ **Auto-scaling** ve **load balancing** yapabilir
- ✅ **CI/CD pipelines** ile automated deployment yapabilir
- ✅ **Monitoring & observability** stack kurabilir
- ✅ **High availability** ve **disaster recovery** sağlayabilirsiniz

**Final Result:** Production-ready, enterprise-grade, cloud-native microservices architecture! 🎉🚀
