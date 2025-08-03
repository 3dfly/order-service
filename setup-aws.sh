#!/bin/bash

echo "🚀 AWS Order Service Setup Script"
echo "================================="

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed. Please install it first:"
    echo "   https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
    exit 1
fi

# Get current AWS account information
echo "🔍 Checking current AWS configuration..."
CURRENT_ACCOUNT=$(aws sts get-caller-identity --query 'Account' --output text 2>/dev/null || echo "Not configured")
CURRENT_REGION=$(aws configure get region 2>/dev/null || echo "Not configured")

echo "Current AWS Account: $CURRENT_ACCOUNT"
echo "Current AWS Region: $CURRENT_REGION"
echo ""

# Ask user to confirm/configure AWS account
read -p "Is this your personal AWS account? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Please configure your personal AWS account first:"
    echo "1. Run: aws configure"
    echo "2. Enter your personal AWS credentials"
    echo "3. Choose your preferred region (e.g., us-east-1, eu-west-1)"
    echo "4. Re-run this script"
    exit 1
fi

# Set variables
AWS_ACCOUNT_ID=$CURRENT_ACCOUNT
AWS_REGION=$CURRENT_REGION

echo ""
echo "📋 Configuration Summary:"
echo "========================"
echo "AWS Account ID: $AWS_ACCOUNT_ID"
echo "AWS Region: $AWS_REGION"
echo "ECR Repository: order-service"
echo "ECS Cluster: order-service-cluster"
echo ""

read -p "Continue with deployment? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 0
fi

echo ""
echo "🏗️ Starting AWS infrastructure setup..."

# Create ECR repository
echo "📦 Creating ECR repository..."
aws ecr create-repository --repository-name order-service --region $AWS_REGION 2>/dev/null || echo "ECR repository already exists"

# Build and push Docker image
echo "🐳 Building Docker image..."
docker build -t order-service .

echo "🔐 Logging into ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

echo "📤 Pushing image to ECR..."
docker tag order-service:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest

# Create task definition with correct account ID and production environment
echo "📝 Creating ECS task definition..."
cat > task-definition.json << EOF
{
  "family": "order-service-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::$AWS_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::$AWS_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "order-service",
      "image": "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/order-service",
          "awslogs-region": "$AWS_REGION",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "production"
        },
        {
          "name": "DB_HOST",
          "value": "order-service-db.$AWS_REGION.rds.amazonaws.com"
        },
        {
          "name": "DB_PORT",
          "value": "3306"
        },
        {
          "name": "DB_NAME",
          "value": "order_service_db"
        },
        {
          "name": "DB_USERNAME",
          "value": "$DB_USERNAME"
        },
        {
          "name": "DB_PASSWORD",
          "value": "$DB_PASSWORD"
        }
      ]
    }
  ]
}
EOF

# Create CloudWatch log group
echo "📊 Creating CloudWatch log group..."
aws logs create-log-group --log-group-name /ecs/order-service --region $AWS_REGION 2>/dev/null || echo "Log group already exists"

# Create ECS cluster
echo "🏢 Creating ECS cluster..."
aws ecs create-cluster --cluster-name order-service-cluster --region $AWS_REGION 2>/dev/null || echo "Cluster already exists"

# Register task definition
echo "📋 Registering task definition..."
aws ecs register-task-definition --cli-input-json file://task-definition.json --region $AWS_REGION

# Get VPC and subnet information
echo "🌐 Getting VPC and subnet information..."
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --region $AWS_REGION --query 'Vpcs[0].VpcId' --output text)
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --region $AWS_REGION --query 'Subnets[*].SubnetId' --output text)

echo "VPC ID: $VPC_ID"
echo "Subnet IDs: $SUBNET_IDS"

# Create security groups
echo "🔐 Creating security groups..."
ALB_SG_ID=$(aws ec2 create-security-group \
    --group-name order-service-alb-sg \
    --description "Security group for Order Service ALB" \
    --vpc-id $VPC_ID \
    --region $AWS_REGION \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups --filters "Name=group-name,Values=order-service-alb-sg" --region $AWS_REGION --query 'SecurityGroups[0].GroupId' --output text)

# Allow HTTP traffic to ALB
aws ec2 authorize-security-group-ingress \
    --group-id $ALB_SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0 \
    --region $AWS_REGION 2>/dev/null || echo "ALB ingress rule already exists"

ECS_SG_ID=$(aws ec2 create-security-group \
    --group-name order-service-ecs-sg \
    --description "Security group for Order Service ECS" \
    --vpc-id $VPC_ID \
    --region $AWS_REGION \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups --filters "Name=group-name,Values=order-service-ecs-sg" --region $AWS_REGION --query 'SecurityGroups[0].GroupId' --output text)

# Allow traffic from ALB to ECS
aws ec2 authorize-security-group-ingress \
    --group-id $ECS_SG_ID \
    --protocol tcp \
    --port 8080 \
    --source-group $ALB_SG_ID \
    --region $AWS_REGION 2>/dev/null || echo "ECS ingress rule already exists"

# Create Load Balancer
echo "⚖️ Creating Application Load Balancer..."
SUBNET_ARRAY=$(echo $SUBNET_IDS | tr ' ' ',')

ALB_ARN=$(aws elbv2 create-load-balancer \
    --name order-service-alb \
    --subnets $(echo $SUBNET_IDS) \
    --security-groups $ALB_SG_ID \
    --region $AWS_REGION \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text 2>/dev/null || \
    aws elbv2 describe-load-balancers --names order-service-alb --region $AWS_REGION --query 'LoadBalancers[0].LoadBalancerArn' --output text)

# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers --load-balancer-arns $ALB_ARN --region $AWS_REGION --query 'LoadBalancers[0].DNSName' --output text)

# Create Target Group
echo "🎯 Creating target group..."
TG_ARN=$(aws elbv2 create-target-group \
    --name order-service-tg \
    --protocol HTTP \
    --port 8080 \
    --vpc-id $VPC_ID \
    --target-type ip \
    --health-check-path /health \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --region $AWS_REGION \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text 2>/dev/null || \
    aws elbv2 describe-target-groups --names order-service-tg --region $AWS_REGION --query 'TargetGroups[0].TargetGroupArn' --output text)

# Create ALB Listener
echo "👂 Creating ALB listener..."
aws elbv2 create-listener \
    --load-balancer-arn $ALB_ARN \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=$TG_ARN \
    --region $AWS_REGION 2>/dev/null || echo "Listener already exists"

# Create ECS Service
echo "🚢 Creating ECS service..."
aws ecs create-service \
    --cluster order-service-cluster \
    --service-name order-service \
    --task-definition order-service-task \
    --desired-count 1 \
    --launch-type FARGATE \
    --platform-version LATEST \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_ARRAY],securityGroups=[$ECS_SG_ID],assignPublicIp=ENABLED}" \
    --load-balancers "targetGroupArn=$TG_ARN,containerName=order-service,containerPort=8080" \
    --region $AWS_REGION 2>/dev/null || echo "Service already exists"

# Clean up temporary file
rm -f task-definition.json

echo ""
echo "🎉 Deployment completed successfully!"
echo "=================================="
echo ""
echo "📋 Your AWS Resources:"
echo "• Account ID: $AWS_ACCOUNT_ID"
echo "• Region: $AWS_REGION" 
echo "• ECS Cluster: order-service-cluster"
echo "• Load Balancer: $ALB_DNS"
echo ""
echo "🌐 Your Order Service will be available at:"
echo "   http://$ALB_DNS"
echo ""
echo "📋 Available API endpoints (wait 2-3 minutes for startup):"
echo ""
echo "🛒 Order Management:"
echo "• POST   http://$ALB_DNS/orders                    (Create order)"
echo "• GET    http://$ALB_DNS/orders                    (Get all orders)"
echo "• GET    http://$ALB_DNS/orders/{id}               (Get order by ID)"
echo "• PUT    http://$ALB_DNS/orders/{id}               (Update order)"
echo "• DELETE http://$ALB_DNS/orders/{id}               (Delete order)"
echo "• PATCH  http://$ALB_DNS/orders/{id}/status?status=ACCEPTED  (Update status)"
echo "• GET    http://$ALB_DNS/orders/customer/{id}      (Orders by customer)"
echo "• GET    http://$ALB_DNS/orders/status/PENDING     (Orders by status)"
echo ""
echo "🏪 Seller Management:"
echo "• POST   http://$ALB_DNS/sellers                   (Create seller)"
echo "• GET    http://$ALB_DNS/sellers                   (Get all sellers)"
echo "• GET    http://$ALB_DNS/sellers/{id}              (Get seller by ID)"
echo "• GET    http://$ALB_DNS/sellers/user/{id}         (Get seller by user ID)"
echo "• GET    http://$ALB_DNS/sellers/verified          (Get verified sellers)"
echo "• PATCH  http://$ALB_DNS/sellers/{id}/verify       (Verify seller)"
echo "• DELETE http://$ALB_DNS/sellers/{id}              (Delete seller)"
echo ""
echo "🏥 Health & Monitoring:"
echo "• GET    http://$ALB_DNS/health                    (Health check)"
echo "• GET    http://$ALB_DNS/health/ready              (Readiness check)"
echo ""
echo "⏳ Please wait 2-3 minutes for the service to be fully deployed and healthy..."
echo ""
echo "🔧 To test when ready:"
echo "   curl http://$ALB_DNS/health" 