#!/bin/bash

echo "🚀 Deploying Order Service with 3D Printing Functionality"
echo "========================================================="

# Check if we're in the right directory
if [ ! -f "README.md" ] || [ ! -f "build.gradle" ]; then
    echo "❌ Error: Not in the order-service directory!"
    echo "Please run this script from the project root"
    exit 1
fi

# Set AWS configuration (update these with your values)
AWS_ACCOUNT_ID="881043647139"
AWS_REGION="eu-west-1"
ECR_REPOSITORY="order-service"
ECS_CLUSTER="order-service-cluster"
ECS_SERVICE="order-service"

echo "📋 Configuration:"
echo "AWS Account ID: $AWS_ACCOUNT_ID"
echo "AWS Region: $AWS_REGION"
echo "ECR Repository: $ECR_REPOSITORY"
echo "ECS Cluster: $ECS_CLUSTER"
echo "ECS Service: $ECS_SERVICE"
echo ""

# Confirm deployment
read -p "Continue with deployment? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 0
fi

echo ""
echo "🏗️ Starting deployment process..."

# Step 1: Build the application
echo "📦 Building Spring Boot application..."
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Step 2: Build Docker image
echo "🐳 Building Docker image with slicer..."
docker build -t $ECR_REPOSITORY:latest . --platform linux/amd64
if [ $? -ne 0 ]; then
    echo "❌ Docker build failed!"
    exit 1
fi

# Step 3: Login to ECR
echo "🔐 Logging into Amazon ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
if [ $? -ne 0 ]; then
    echo "❌ ECR login failed!"
    exit 1
fi

# Step 4: Tag and push image
echo "📤 Pushing image to ECR..."
docker tag $ECR_REPOSITORY:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest
if [ $? -ne 0 ]; then
    echo "❌ Docker push failed!"
    exit 1
fi

# Step 5: Update task definition
echo "📝 Updating ECS task definition..."
aws ecs register-task-definition \
    --cli-input-json file://task-definition-h2.json \
    --region $AWS_REGION
if [ $? -ne 0 ]; then
    echo "❌ Task definition update failed!"
    exit 1
fi

# Step 6: Update ECS service
echo "🔄 Updating ECS service..."
aws ecs update-service \
    --cluster $ECS_CLUSTER \
    --service $ECS_SERVICE \
    --force-new-deployment \
    --region $AWS_REGION
if [ $? -ne 0 ]; then
    echo "❌ Service update failed!"
    exit 1
fi

echo "⏳ Waiting for deployment to complete..."
aws ecs wait services-stable \
    --cluster $ECS_CLUSTER \
    --services $ECS_SERVICE \
    --region $AWS_REGION

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 SUCCESS! Order Service with 3D Printing is now deployed!"
    echo ""
    echo "🌐 Application URL: http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com"
    echo ""
    echo "🧪 Test the printing calculation endpoint:"
    echo "curl -X POST \\"
    echo "  http://order-service-alb-971782851.eu-west-1.elb.amazonaws.com/orders/calculate \\"
    echo "  -F \"stlFile=@your-model.stl\" \\"
    echo "  -H \"Content-Type: multipart/form-data\""
    echo ""
    echo "📝 Available endpoints:"
    echo "  - Health check: GET /health"
    echo "  - Orders: GET /orders"
    echo "  - Calculate price: POST /orders/calculate (with STL file)"
    echo ""
    echo "💰 Pricing configuration:"
    echo "  - Price per gram: $0.05"
    echo "  - Price per minute: $0.10"
    echo ""
else
    echo "❌ Deployment failed during service stabilization!"
    echo "Check the ECS console for details."
    exit 1
fi 