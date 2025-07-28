# Order Service

A Spring Boot microservice for managing orders with comprehensive health monitoring and error handling.

## 🚀 Live API

**Base URL:** `http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com`

## 📋 API Endpoints

### Order Management
| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| `GET` | `/orders` | Retrieve all orders | `"Here are your orders!"` |
| `GET` | `/orders/calculate` | Calculate order summary | `"Calculating order summary..."` |

### Health & Monitoring
| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| `GET` | `/health` | Service health check with database status | JSON with status, timestamp, and database info |
| `GET` | `/health/ready` | Kubernetes/container readiness probe | JSON with ready status and timestamp |

### Error Handling
- Custom error controller handles undefined routes
- Returns JSON error responses with appropriate HTTP status codes
- Example: `GET /nonexistent` → `{"error":"Not Found","path":"unknown"}` (404)

## 🔧 Technology Stack

- **Framework:** Spring Boot 3.5.4
- **Java Version:** 21
- **Build Tool:** Gradle
- **Container:** Docker (Multi-stage build)
- **Cloud Platform:** AWS ECS Fargate
- **Load Balancer:** Application Load Balancer
- **Container Registry:** Amazon ECR

## 🏗️ Architecture

```
Internet → ALB → ECS Fargate → Spring Boot App (Port 8080)
           ↓
       Target Group
    (Health Check: /health)
```

## 🛠️ Development

### Prerequisites
- Java 21
- Docker
- AWS CLI (for deployment)

### Local Development

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd order-service
   ```

2. **Run locally:**
   ```bash
   ./gradlew bootRun
   ```

3. **Test endpoints:**
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/orders
   ```

### Docker Development

1. **Build image:**
   ```bash
   docker build -t order-service .
   ```

2. **Run container:**
   ```bash
   docker run -p 8080:8080 order-service
   ```

## 🚀 Deployment

### AWS Deployment (Current)

The service is deployed on AWS using:
- **ECS Cluster:** `order-service-cluster`
- **Task Definition:** `order-service-task`
- **Service:** Fargate with 1 task
- **Region:** `eu-west-1`

### Deployment Commands

```bash
# Build and tag image
docker build -t order-service .
docker tag order-service:latest 881043647139.dkr.ecr.eu-west-1.amazonaws.com/order-service:latest

# Push to ECR
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin 881043647139.dkr.ecr.eu-west-1.amazonaws.com
docker push 881043647139.dkr.ecr.eu-west-1.amazonaws.com/order-service:latest

# Update ECS service
aws ecs update-service --cluster order-service-cluster --service order-service --force-new-deployment --region eu-west-1
```

## 🧪 Testing

### Manual Testing

```bash
# Health check
curl http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com/health

# Orders API
curl http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com/orders
curl http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com/orders/calculate

# Error handling
curl http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com/nonexistent
```

### Expected Responses

**Health Check:**
```json
{
  "status": "UP",
  "timestamp": "2025-07-28T15:06:24.669277150",
  "service": "order-service",
  "database": "NOT_CONFIGURED"
}
```

**Readiness Check:**
```json
{
  "ready": true,
  "timestamp": "2025-07-28T15:06:35.554445981"
}
```

## 📁 Project Structure

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/com/threedfly/orderservice/
│   │   │   ├── controller/
│   │   │   │   ├── OrderController.java      # Order management endpoints
│   │   │   │   ├── HealthController.java     # Health monitoring
│   │   │   │   └── CustomErrorController.java # Error handling
│   │   │   └── OrderServiceApplication.java  # Main application
│   │   └── resources/
│   │       └── application.properties        # Spring configuration
│   └── test/
├── Dockerfile                               # Multi-stage Docker build
├── build.gradle                            # Gradle build configuration
├── settings.gradle                         # Gradle settings
└── README.md                              # This file
```

## 🔐 Security

- **Container Security:** Non-root user in Docker container
- **Network Security:** Security groups restrict access
- **Load Balancer:** Only HTTP (port 80) exposed publicly
- **ECS Tasks:** Run in private subnets with public IP for ECR access

## 📊 Monitoring

- **Health Endpoint:** `/health` - Comprehensive service status
- **Readiness Probe:** `/health/ready` - Container readiness
- **CloudWatch Logs:** Application logs available in `/ecs/order-service`
- **ALB Health Checks:** Automatic target health monitoring

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Commit your changes: `git commit -am 'Add some feature'`
5. Push to the branch: `git push origin feature/your-feature-name`
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For issues and questions:
- Create an issue in this repository
- Check the application logs in CloudWatch
- Verify service health at `/health` endpoint 