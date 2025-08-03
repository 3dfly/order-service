# 🚀 Production Deployment Guide - Payment API

## 📋 Pre-requisites

### 1. PayPal Business Account Setup
```bash
# 1. Create PayPal Business Account
# 2. Get API Credentials from PayPal Developer Dashboard:
#    - Go to https://developer.paypal.com/
#    - Create an app
#    - Get Client ID and Client Secret
```

### 2. Environment Variables for Production
```bash
# Add to your deployment environment:
export PAYPAL_CLIENT_ID="your_production_client_id"
export PAYPAL_CLIENT_SECRET="your_production_client_secret"
export PAYPAL_MODE="live"  # Use "sandbox" for testing
export PAYPAL_BASE_URL="https://api.paypal.com"  # Production URL

# Payment Configuration
export PAYMENT_PLATFORM_FEE="3.00"  # Your platform fee in USD
export PAYMENT_CURRENCY="USD"

# Database (if using MySQL in production)
export SPRING_DATASOURCE_URL="jdbc:mysql://your-db-host:3306/orderdb"
export SPRING_DATASOURCE_USERNAME="your_username"
export SPRING_DATASOURCE_PASSWORD="your_password"
```

## 🔧 AWS ECS Deployment

### 1. Update task-definition-h2.json
```json
{
  "family": "order-service-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::YOUR_ACCOUNT:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "order-service",
      "image": "YOUR_ACCOUNT.dkr.ecr.REGION.amazonaws.com/order-service:latest",
      "portMappings": [{"containerPort": 8080}],
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "production"},
        {"name": "PAYPAL_CLIENT_ID", "value": "YOUR_PAYPAL_CLIENT_ID"},
        {"name": "PAYPAL_CLIENT_SECRET", "value": "YOUR_PAYPAL_CLIENT_SECRET"},
        {"name": "PAYPAL_MODE", "value": "live"},
        {"name": "PAYPAL_BASE_URL", "value": "https://api.paypal.com"},
        {"name": "PAYMENT_PLATFORM_FEE", "value": "3.00"},
        {"name": "PAYMENT_CURRENCY", "value": "USD"}
      ]
    }
  ]
}
```

### 2. Deploy to AWS
```bash
# Build and deploy
./deploy-printing-update.sh

# Or manual deployment:
docker build -t order-service .
docker tag order-service:latest $AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest
docker push $AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest

aws ecs update-service \
  --cluster order-service-cluster \
  --service order-service \
  --force-new-deployment
```

## 🔗 PayPal Webhook Setup (Optional but Recommended)

### 1. Configure Webhook in PayPal Dashboard
```
Webhook URL: https://your-domain.com/payments/webhook/paypal
Events to subscribe to:
- PAYMENT.SALE.COMPLETED
- PAYMENT.SALE.DENIED  
- PAYMENT.SALE.REFUNDED
```

### 2. Webhook Processing (Already Implemented)
The API already has a webhook endpoint:
```
POST /payments/webhook/paypal
```

## 📊 Testing the Production API

### 1. Test Payment Flow
```bash
# 1. Create Seller
curl -X POST "https://your-domain.com/sellers" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "businessName": "My Store",
    "businessAddress": "123 Business St",
    "contactEmail": "seller@mystore.com",
    "contactPhone": "+15551234567"
  }'

# 2. Create Order  
curl -X POST "https://your-domain.com/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2001,
    "customerName": "John Doe", 
    "customerEmail": "john@example.com",
    "productId": 3001,
    "quantity": 1,
    "totalPrice": 50.00,
    "shippingAddress": "456 Customer Ave",
    "supplierId": 4001,
    "sellerId": 1
  }'

# 3. Create Payment (will redirect to PayPal)
curl -X POST "https://your-domain.com/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "method": "PAYPAL",
    "totalAmount": 50.00,
    "paypalEmail": "buyer@example.com",
    "currency": "USD",
    "description": "Order payment",
    "successUrl": "https://yourapp.com/success",
    "cancelUrl": "https://yourapp.com/cancel"
  }'

# Response will include paypalApprovalUrl for customer redirect
```

### 2. Payment Splitting Verification
```bash
# Query payments by seller
curl "https://your-domain.com/payments/seller/1"

# Expected response shows automatic splitting:
[
  {
    "id": 1,
    "totalAmount": 50.00,
    "platformFee": 3.00,     # Your platform revenue
    "sellerAmount": 47.00,   # Seller receives
    "status": "COMPLETED"
  }
]
```

## 📈 Monitoring & Analytics

### 1. Platform Revenue Tracking
```sql
-- Total platform fees collected
SELECT SUM(platform_fee) as total_platform_revenue 
FROM payments 
WHERE status = 'COMPLETED';

-- Revenue by time period
SELECT DATE(created_at) as date, 
       SUM(platform_fee) as daily_platform_revenue
FROM payments 
WHERE status = 'COMPLETED'
GROUP BY DATE(created_at);
```

### 2. Seller Analytics
```sql
-- Top earning sellers
SELECT s.business_name, 
       SUM(p.seller_amount) as total_earnings
FROM payments p
JOIN seller s ON p.seller_id = s.id
WHERE p.status = 'COMPLETED'
GROUP BY s.id
ORDER BY total_earnings DESC;
```

## 🔒 Security Considerations

### 1. PayPal Security
- ✅ OAuth 2.0 authentication implemented
- ✅ No sensitive payment data stored locally
- ✅ PCI compliance through PayPal

### 2. API Security
- ✅ Input validation on all endpoints
- ✅ SQL injection protection via JPA
- ✅ Global exception handling
- 🔜 Add rate limiting (recommended)
- 🔜 Add API authentication (recommended)

## 🎯 Success Metrics

After deployment, monitor:
- **Payment Success Rate**: Target >95%
- **Platform Fee Collection**: $3 per transaction
- **Seller Payout Accuracy**: 100% correct splitting
- **API Response Times**: <2 seconds
- **PayPal Approval Conversion**: Track completion rates

## 🚀 Scaling Considerations

As your marketplace grows:
1. **Database Scaling**: Consider PostgreSQL or MySQL with read replicas
2. **Payment Processing**: Implement retry logic for failed payments
3. **Multi-Currency**: Extend for international markets
4. **Multiple Payment Methods**: Add Stripe, Apple Pay, Google Pay
5. **Seller Onboarding**: Automate seller verification

---

## ✅ Launch Checklist

- [ ] PayPal production credentials configured
- [ ] Environment variables set in deployment
- [ ] Database backup strategy implemented
- [ ] Monitoring and alerting configured
- [ ] Payment webhook tested
- [ ] SSL certificate installed
- [ ] Load balancer health checks configured
- [ ] Payment flow end-to-end tested
- [ ] Seller payout process verified
- [ ] Customer support documentation updated

**Your marketplace payment system is ready for production! 🎉** 