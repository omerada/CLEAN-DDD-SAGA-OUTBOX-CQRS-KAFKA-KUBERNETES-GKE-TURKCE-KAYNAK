# 🎯 Kubernetes Fundamentals - Hands-On Exercises

## 📋 Exercise Overview

Bu hands-on exercises mikroservisleri Kubernetes cluster'da deploy etmek ve yönetmek için pratik deneyim kazandıracak.

## 🚀 Exercise 1: Local Cluster Setup & Verification

### Goal

Local Kubernetes cluster kurmak ve temel komutları öğrenmek.

### Tasks

```bash
# 1. Cluster kurulumu (Docker Desktop kullanıyorsak)
# Docker Desktop > Settings > Kubernetes > Enable

# 2. Cluster durumunu kontrol et
kubectl version --short
kubectl cluster-info
kubectl get nodes -o wide

# 3. Namespace oluştur
kubectl create namespace microservices-lab
kubectl get namespaces

# 4. Context switch
kubectl config set-context --current --namespace=microservices-lab
kubectl config get-contexts
```

### Expected Output

```
Client Version: v1.28.0
Server Version: v1.28.0
Kubernetes control plane is running at https://kubernetes.docker.internal:6443

NAME             STATUS   ROLES           AGE   VERSION
docker-desktop   Ready    control-plane   1d    v1.28.0
```

### Verification

```bash
kubectl get all -n microservices-lab
# Should show: No resources found
```

---

## 🔧 Exercise 2: ConfigMap & Secrets Management

### Goal

Configuration management patterns'ını hands-on öğrenmek.

### Tasks

#### Step 1: Create ConfigMap

```bash
# Imperative way
kubectl create configmap app-config \
  --from-literal=DATABASE_URL=postgresql://postgres:5432/app_db \
  --from-literal=LOG_LEVEL=DEBUG \
  -n microservices-lab

# Verify
kubectl get configmaps -n microservices-lab
kubectl describe configmap app-config -n microservices-lab
```

#### Step 2: Create from File

```yaml
# config.properties
database.host=postgres-service
database.port=5432
database.name=app_db
logging.level=INFO
cache.enabled=true
cache.ttl=3600
```

```bash
# Create file and configmap
echo "database.host=postgres-service
database.port=5432
database.name=app_db
logging.level=INFO
cache.enabled=true
cache.ttl=3600" > config.properties

kubectl create configmap file-config \
  --from-file=config.properties \
  -n microservices-lab

kubectl get configmap file-config -o yaml -n microservices-lab
```

#### Step 3: Create Secrets

```bash
# Create secret imperatively
kubectl create secret generic db-secret \
  --from-literal=username=admin \
  --from-literal=password=secretpassword \
  -n microservices-lab

# Verify (password will be base64 encoded)
kubectl get secret db-secret -o yaml -n microservices-lab

# Decode secret
kubectl get secret db-secret -n microservices-lab -o jsonpath='{.data.password}' | base64 --decode
```

### Verification Commands

```bash
kubectl get configmaps,secrets -n microservices-lab
kubectl describe configmap app-config -n microservices-lab
kubectl get secret db-secret -o yaml -n microservices-lab
```

---

## 🐘 Exercise 3: PostgreSQL Deployment

### Goal

Stateful application deployment ve persistent storage'ı öğrenmek.

### Tasks

#### Step 1: Create PersistentVolumeClaim

```yaml
# postgres-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: microservices-lab
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

```bash
kubectl apply -f postgres-pvc.yaml
kubectl get pvc -n microservices-lab
```

#### Step 2: Deploy PostgreSQL

```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: microservices-lab
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:15-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: "testdb"
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: username
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          volumeMounts:
            - name: postgres-storage
              mountPath: /var/lib/postgresql/data
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
      volumes:
        - name: postgres-storage
          persistentVolumeClaim:
            claimName: postgres-pvc
```

```bash
kubectl apply -f postgres-deployment.yaml
kubectl get pods -n microservices-lab -w
```

#### Step 3: Create Service

```yaml
# postgres-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: microservices-lab
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
  type: ClusterIP
```

```bash
kubectl apply -f postgres-service.yaml
kubectl get services -n microservices-lab
```

### Verification

```bash
# Check pod status
kubectl get pods -n microservices-lab
kubectl describe pod -l app=postgres -n microservices-lab

# Check logs
kubectl logs -l app=postgres -n microservices-lab

# Test connection
kubectl run postgres-test --image=postgres:15-alpine -i --tty --rm --restart=Never -n microservices-lab -- psql -h postgres-service -U admin -d testdb

# Inside psql:
# \l  (list databases)
# \q  (quit)
```

---

## 📦 Exercise 4: Simple Web Application Deployment

### Goal

Stateless application deployment ve service exposure'ı öğrenmek.

### Tasks

#### Step 1: Create Simple App

```yaml
# simple-app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: simple-app
  namespace: microservices-lab
spec:
  replicas: 3
  selector:
    matchLabels:
      app: simple-app
  template:
    metadata:
      labels:
        app: simple-app
    spec:
      containers:
        - name: app
          image: nginx:alpine
          ports:
            - containerPort: 80
          env:
            - name: CONFIG_VALUE
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: LOG_LEVEL
          volumeMounts:
            - name: config-volume
              mountPath: /etc/config
            - name: html-volume
              mountPath: /usr/share/nginx/html
          resources:
            requests:
              memory: "64Mi"
              cpu: "100m"
            limits:
              memory: "128Mi"
              cpu: "200m"
          livenessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
      volumes:
        - name: config-volume
          configMap:
            name: file-config
        - name: html-volume
          configMap:
            name: html-config

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: html-config
  namespace: microservices-lab
data:
  index.html: |
    <!DOCTYPE html>
    <html>
    <head>
        <title>Kubernetes Lab</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 40px; background: #f0f0f0; }
            .container { background: white; padding: 20px; border-radius: 10px; }
            .status { color: green; font-weight: bold; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🚀 Kubernetes Microservices Lab</h1>
            <p class="status">✅ Application is running successfully!</p>
            <h2>Pod Information:</h2>
            <p><strong>Hostname:</strong> <span id="hostname"></span></p>
            <p><strong>Environment:</strong> Kubernetes Lab</p>
            <p><strong>Timestamp:</strong> <span id="timestamp"></span></p>
        </div>
        
        <script>
            document.getElementById('hostname') = window.location.hostname;
            document.getElementById('timestamp') = new Date().toLocaleString();
        </script>
    </body>
    </html>
```

```bash
kubectl apply -f simple-app-deployment.yaml
kubectl get pods -n microservices-lab -l app=simple-app
```

#### Step 2: Create Services

```yaml
# simple-app-services.yaml
apiVersion: v1
kind: Service
metadata:
  name: simple-app-clusterip
  namespace: microservices-lab
spec:
  selector:
    app: simple-app
  ports:
    - port: 80
      targetPort: 80
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: simple-app-nodeport
  namespace: microservices-lab
spec:
  selector:
    app: simple-app
  ports:
    - port: 80
      targetPort: 80
      nodePort: 30080
  type: NodePort

---
apiVersion: v1
kind: Service
metadata:
  name: simple-app-loadbalancer
  namespace: microservices-lab
spec:
  selector:
    app: simple-app
  ports:
    - port: 80
      targetPort: 80
  type: LoadBalancer
```

```bash
kubectl apply -f simple-app-services.yaml
kubectl get services -n microservices-lab
```

### Verification

```bash
# Check all resources
kubectl get all -n microservices-lab

# Test ClusterIP service
kubectl run test-pod --image=curlimages/curl -i --tty --rm --restart=Never -n microservices-lab -- curl http://simple-app-clusterip

# Test NodePort (local)
curl http://localhost:30080

# Check endpoints
kubectl get endpoints -n microservices-lab
```

---

## 🔍 Exercise 5: Health Checks & Scaling

### Goal

Application health monitoring ve horizontal scaling'i öğrenmek.

### Tasks

#### Step 1: Add Health Checks

```yaml
# health-app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: health-app
  namespace: microservices-lab
spec:
  replicas: 2
  selector:
    matchLabels:
      app: health-app
  template:
    metadata:
      labels:
        app: health-app
    spec:
      containers:
        - name: app
          image: nginx:alpine
          ports:
            - containerPort: 80
          volumeMounts:
            - name: health-config
              mountPath: /etc/nginx/conf.d
          livenessProbe:
            httpGet:
              path: /health
              port: 80
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /ready
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /startup
              port: 80
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          resources:
            requests:
              memory: "64Mi"
              cpu: "100m"
            limits:
              memory: "128Mi"
              cpu: "200m"
      volumes:
        - name: health-config
          configMap:
            name: health-nginx-config

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: health-nginx-config
  namespace: microservices-lab
data:
  default.conf: |
    server {
        listen 80;
        
        location / {
            return 200 'Hello from Health App!';
            add_header Content-Type text/plain;
        }
        
        location /health {
            access_log off;
            return 200 'healthy';
            add_header Content-Type text/plain;
        }
        
        location /ready {
            access_log off;
            return 200 'ready';
            add_header Content-Type text/plain;
        }
        
        location /startup {
            access_log off;
            return 200 'started';
            add_header Content-Type text/plain;
        }
    }
```

```bash
kubectl apply -f health-app-deployment.yaml
kubectl get pods -n microservices-lab -l app=health-app -w
```

#### Step 2: Scaling Operations

```bash
# Manual scaling
kubectl scale deployment health-app --replicas=5 -n microservices-lab
kubectl get pods -n microservices-lab -l app=health-app

# Check rollout status
kubectl rollout status deployment/health-app -n microservices-lab

# Scale down
kubectl scale deployment health-app --replicas=2 -n microservices-lab

# Auto-restart (rolling update)
kubectl rollout restart deployment/health-app -n microservices-lab
```

#### Step 3: Test Health Checks

```bash
# Create service for testing
kubectl expose deployment health-app --port=80 --type=NodePort -n microservices-lab

# Get service details
kubectl get services -n microservices-lab

# Test endpoints
kubectl run test-health --image=curlimages/curl -i --tty --rm --restart=Never -n microservices-lab -- sh

# Inside test pod:
curl http://health-app/health
curl http://health-app/ready
curl http://health-app/startup
```

### Verification

```bash
# Check pod health status
kubectl describe pods -l app=health-app -n microservices-lab

# Watch pod events
kubectl get events -n microservices-lab --sort-by=.metadata.creationTimestamp

# Check resource usage
kubectl top pods -n microservices-lab
kubectl top nodes
```

---

## 🌐 Exercise 6: Ingress & External Access

### Goal

External traffic routing ve ingress controller'ı öğrenmek.

### Tasks

#### Step 1: Install Ingress Controller (if not exists)

```bash
# For Docker Desktop
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Wait for controller
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

#### Step 2: Create Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: lab-ingress
  namespace: microservices-lab
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: lab.local
      http:
        paths:
          - path: /simple
            pathType: Prefix
            backend:
              service:
                name: simple-app-clusterip
                port:
                  number: 80
          - path: /health
            pathType: Prefix
            backend:
              service:
                name: health-app
                port:
                  number: 80

---
# TLS version (optional)
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: lab-ingress-tls
  namespace: microservices-lab
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - lab.local
      secretName: lab-tls-secret
  rules:
    - host: lab.local
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: simple-app-clusterip
                port:
                  number: 80
```

```bash
kubectl apply -f ingress.yaml
kubectl get ingress -n microservices-lab
```

#### Step 3: Test External Access

```bash
# Add to /etc/hosts (or C:\Windows\System32\drivers\etc\hosts on Windows)
# 127.0.0.1 lab.local

# Test ingress
curl -H "Host: lab.local" http://localhost/simple
curl -H "Host: lab.local" http://localhost/health

# Or if DNS is configured
curl http://lab.local/simple
curl http://lab.local/health
```

### Verification

```bash
kubectl describe ingress lab-ingress -n microservices-lab
kubectl get endpoints -n microservices-lab
```

---

## 🔧 Exercise 7: Troubleshooting Practice

### Goal

Common Kubernetes issues'ları debug etmeyi öğrenmek.

### Tasks

#### Step 1: Create Problematic Deployment

```yaml
# problematic-app.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: problematic-app
  namespace: microservices-lab
spec:
  replicas: 2
  selector:
    matchLabels:
      app: problematic-app
  template:
    metadata:
      labels:
        app: problematic-app
    spec:
      containers:
        - name: app
          image: nginx:invalid-tag # Intentional error
          ports:
            - containerPort: 80
          env:
            - name: REQUIRED_CONFIG
              valueFrom:
                configMapKeyRef:
                  name: non-existent-config # Intentional error
                  key: value
          resources:
            requests:
              memory: "10Gi" # Intentional resource error
              cpu: "10"
```

```bash
kubectl apply -f problematic-app.yaml
kubectl get pods -n microservices-lab -l app=problematic-app
```

#### Step 2: Debug Issues

```bash
# 1. Check pod status
kubectl get pods -n microservices-lab -l app=problematic-app

# 2. Describe pod for details
kubectl describe pod -l app=problematic-app -n microservices-lab

# 3. Check events
kubectl get events -n microservices-lab --sort-by=.metadata.creationTimestamp

# 4. Check logs (if pod starts)
kubectl logs -l app=problematic-app -n microservices-lab

# 5. Check resource quotas
kubectl describe resourcequota -n microservices-lab
kubectl top nodes
```

#### Step 3: Fix Issues

```yaml
# fixed-app.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config-fixed
  namespace: microservices-lab
data:
  value: "working-value"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fixed-app
  namespace: microservices-lab
spec:
  replicas: 2
  selector:
    matchLabels:
      app: fixed-app
  template:
    metadata:
      labels:
        app: fixed-app
    spec:
      containers:
        - name: app
          image: nginx:alpine # Fixed image
          ports:
            - containerPort: 80
          env:
            - name: REQUIRED_CONFIG
              valueFrom:
                configMapKeyRef:
                  name: app-config-fixed # Fixed config
                  key: value
          resources:
            requests:
              memory: "64Mi" # Fixed resources
              cpu: "100m"
            limits:
              memory: "128Mi"
              cpu: "200m"
```

```bash
kubectl apply -f fixed-app.yaml
kubectl get pods -n microservices-lab -l app=fixed-app -w
```

### Verification

```bash
# Clean up problematic deployment
kubectl delete deployment problematic-app -n microservices-lab

# Verify fixed app
kubectl get pods -n microservices-lab -l app=fixed-app
kubectl logs -l app=fixed-app -n microservices-lab
```

---

## 🎯 Final Challenge: Complete Microservice

### Goal

Learned concepts'leri combine ederek complete microservice deploy etmek.

### Tasks

Create a complete microservice with:

- ConfigMap for configuration
- Secret for database credentials
- PostgreSQL database
- Health checks
- Service exposure
- Ingress routing

```yaml
# final-challenge.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: final-app-config
  namespace: microservices-lab
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: final-app
      datasource:
        url: jdbc:postgresql://postgres-service:5432/testdb
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
    logging:
      level:
        root: INFO

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: final-app
  namespace: microservices-lab
spec:
  replicas: 2
  selector:
    matchLabels:
      app: final-app
  template:
    metadata:
      labels:
        app: final-app
    spec:
      initContainers:
        - name: wait-for-db
          image: postgres:15-alpine
          command:
            - /bin/sh
            - -c
            - |
              until pg_isready -h postgres-service -p 5432; do
                echo "Waiting for PostgreSQL..."
                sleep 2
              done
      containers:
        - name: app
          image: nginx:alpine
          ports:
            - containerPort: 80
          env:
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          volumeMounts:
            - name: config-volume
              mountPath: /etc/config
            - name: web-content
              mountPath: /usr/share/nginx/html
          livenessProbe:
            httpGet:
              path: /health
              port: 80
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /ready
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "200m"
      volumes:
        - name: config-volume
          configMap:
            name: final-app-config
        - name: web-content
          configMap:
            name: final-web-content

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: final-web-content
  namespace: microservices-lab
data:
  index.html: |
    <!DOCTYPE html>
    <html>
    <head><title>Final Challenge App</title></head>
    <body>
        <h1>🎉 Final Challenge Completed!</h1>
        <p>✅ ConfigMap: Loaded</p>
        <p>✅ Secret: Loaded</p>
        <p>✅ Database: Connected</p>
        <p>✅ Health Checks: Working</p>
        <p>✅ Service: Exposed</p>
        <p>✅ Ingress: Configured</p>
    </body>
    </html>

  health: |
    healthy

  ready: |
    ready

---
apiVersion: v1
kind: Service
metadata:
  name: final-app-service
  namespace: microservices-lab
spec:
  selector:
    app: final-app
  ports:
    - port: 80
      targetPort: 80

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: final-app-ingress
  namespace: microservices-lab
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: final.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: final-app-service
                port:
                  number: 80
```

### Deploy and Test

```bash
# Deploy final challenge
kubectl apply -f final-challenge.yaml

# Wait for deployment
kubectl rollout status deployment/final-app -n microservices-lab

# Test all components
kubectl get all -n microservices-lab
curl -H "Host: final.local" http://localhost/

# Cleanup
kubectl delete namespace microservices-lab
```

---

## ✅ Exercise Completion Checklist

Mark completed exercises:

- [ ] **Exercise 1:** Local cluster setup and verification
- [ ] **Exercise 2:** ConfigMap and Secrets management
- [ ] **Exercise 3:** PostgreSQL deployment with persistence
- [ ] **Exercise 4:** Simple web application with multiple services
- [ ] **Exercise 5:** Health checks and scaling operations
- [ ] **Exercise 6:** Ingress configuration and external access
- [ ] **Exercise 7:** Troubleshooting and debugging practice
- [ ] **Final Challenge:** Complete microservice deployment

## 🎯 Learning Outcomes

After completing these exercises, you should be able to:

- ✅ Set up and manage local Kubernetes clusters
- ✅ Deploy stateful and stateless applications
- ✅ Configure application settings with ConfigMaps and Secrets
- ✅ Implement health checks and monitoring
- ✅ Scale applications horizontally
- ✅ Route external traffic with Ingress
- ✅ Debug common Kubernetes issues
- ✅ Deploy complete microservice architectures

**Next Step:** Production Kubernetes deployment ve Google Cloud GKE! 🚀
