# Order Service

A comprehensive Spring Boot microservice for order management with 3D printing capabilities and payment processing.

## 🚀 Features

### Core Order Management
- **CRUD Operations**: Create, read, update, delete orders
- **Order Status Tracking**: PENDING → PROCESSING → SENT → ACCEPTED
- **Customer Management**: Track orders by customer ID
- **Seller Integration**: Associate orders with verified sellers

### 3D Printing Integration
- **STL File Processing**: Upload and analyze STL files for 3D printing
- **Smart Pricing Calculator**: Real-time cost calculation based on:
  - Material weight (calculated from STL geometry)
  - Printing time (estimated from complexity)
  - Configurable pricing rates
- **Production-Ready Slicer**: Advanced STL analyzer with realistic metrics

### 💳 Payment Processing & Splitting
- **PayPal Integration**: Secure payment processing via PayPal API
- **Automatic Payment Splitting**: 
  - Platform fee: $3.00 (configurable)
  - Remaining amount goes to seller
- **Payment Status Tracking**: PENDING → PROCESSING → COMPLETED/FAILED
- **Multiple Payment Methods**: PayPal, Stripe, Credit Card support
- **Webhook Support**: Real-time payment status updates

### Seller Management
- **Seller Registration**: Complete seller onboarding
- **Verification System**: Seller verification workflow
- **Payment Details**: Seller-specific payment configurations

### Health & Monitoring
- **Health Checks**: Application and database health monitoring
- **Comprehensive Logging**: Detailed operation tracking
- **Error Handling**: Global exception handling with proper HTTP status codes

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend/     │    │   Order Service │    │   PayPal API    │
│   Client App    │◄──►│   (Spring Boot) │◄──►│                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Database      │
                       │   (H2/MySQL)    │
                       └─────────────────┘
```

## 📊 API Endpoints

### Payment API (`/payments`)

#### Create Payment
```http
POST /payments
Content-Type: application/json

{
  "orderId": 1,
  "method": "PAYPAL",
  "totalAmount": 10.00,
  "paypalEmail": "buyer@example.com",
  "currency": "USD",
  "description": "Order payment",
  "successUrl": "https://example.com/success",
  "cancelUrl": "https://example.com/cancel"
}
```

**Response:**
```json
{
  "id": 1,
  "orderId": 1,
  "sellerId": 1,
  "sellerBusinessName": "Electronics Store",
  "totalAmount": 10.00,
  "platformFee": 3.00,
  "sellerAmount": 7.00,
  "status": "PENDING",
  "method": "PAYPAL",
  "paypalPaymentId": "PAY-123456789",
  "paypalApprovalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=EC-123456789",
  "createdAt": "2025-01-31T10:00:00"
}
```

#### Execute Payment (After PayPal Approval)
```http
POST /payments/{paymentId}/execute
Content-Type: application/json

{
  "paypalPaymentId": "PAY-123456789",
  "paypalPayerId": "PAYER123456789"
}
```

#### Get Payment Details
```http
GET /payments/{id}
GET /payments/order/{orderId}
GET /payments/seller/{sellerId}
```

### Order API (`/orders`)

#### Create Order
```http
POST /orders
Content-Type: application/json

{
  "customerId": 1001,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "productId": 2001,
  "quantity": 2,
  "totalPrice": 199.99,
  "shippingAddress": "123 Main St, City",
  "supplierId": 3001,
  "sellerId": 1
}
```

#### 3D Printing Cost Calculator
```http
POST /orders/calculate
Content-Type: multipart/form-data

stlFile: [Binary STL file]
```

**Response:**
```json
{
  "filename": "model.stl",
  "status": "SUCCESS",
  "totalPrice": 5.09,
  "weightGrams": 13.8,
  "printingTimeMinutes": 44,
  "pricePerGram": 0.05,
  "pricePerMinute": 0.10,
  "weightCost": 0.69,
  "timeCost": 4.40
}
```

### Seller API (`/sellers`)

#### Register Seller
```http
POST /sellers
Content-Type: application/json

{
  "userId": 1001,
  "businessName": "Electronics Store",
  "businessAddress": "123 Business St",
  "contactEmail": "seller@store.com",
  "contactPhone": "+15551234567"
}
```

## 💰 Payment Flow

### 1. Create Order
```bash
# Customer creates an order
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "productId": 2001,
    "quantity": 1,
    "totalPrice": 25.00,
    "shippingAddress": "123 Main St",
    "supplierId": 3001,
    "sellerId": 1
  }'
```

### 2. Initiate Payment
```bash
# Create payment with automatic splitting
curl -X POST "http://localhost:8080/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "method": "PAYPAL",
    "totalAmount": 25.00,
    "paypalEmail": "buyer@example.com",
    "currency": "USD",
    "successUrl": "https://yourapp.com/success",
    "cancelUrl": "https://yourapp.com/cancel"
  }'
```

### 3. Payment Splitting
- **Total Amount**: $25.00
- **Platform Fee**: $3.00 (goes to your account)
- **Seller Amount**: $22.00 (goes to seller)

### 4. PayPal Redirect
Customer is redirected to PayPal approval URL for secure payment.

### 5. Execute Payment
```bash
# After PayPal approval, execute the payment
curl -X POST "http://localhost:8080/payments/1/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "paypalPaymentId": "PAY-123456789",
    "paypalPayerId": "PAYER123456789"
  }'
```

## 🔧 Configuration

### Application Properties
```properties
# Payment Configuration
payment.platform.fee=3.00
payment.platform.currency=USD

# PayPal Configuration (Sandbox)
paypal.client.id=YOUR_PAYPAL_CLIENT_ID
paypal.client.secret=YOUR_PAYPAL_CLIENT_SECRET
paypal.mode=sandbox
paypal.base.url=https://sandbox.paypal.com

# 3D Printing Configuration
printing.price.per-gram=0.05
printing.price.per-minute=0.10
```

### Environment Variables (Production)
```bash
# PayPal Configuration
PAYPAL_CLIENT_ID=your_production_client_id
PAYPAL_CLIENT_SECRET=your_production_client_secret
PAYPAL_MODE=live
PAYPAL_BASE_URL=https://api.paypal.com

# Payment Configuration
PAYMENT_PLATFORM_FEE=3.00
PAYMENT_CURRENCY=USD
```

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Test Payment API
```bash
# Test with curl (after starting the application)
curl -X POST "http://localhost:8080/payments" \
  -H "Content-Type: application/json" \
  -d @test-payment.json
```

## 🚀 Deployment

### Local Development
```bash
./gradlew bootRun
```

### AWS Deployment
```bash
# Build and deploy to AWS ECS
./deploy-printing-update.sh
```

## 📈 Monitoring

### Health Checks
- **Application Health**: `GET /health`
- **Readiness Check**: `GET /health/ready`
- **Database Status**: Included in health response

### Payment Monitoring
- **Payment Status**: Track via payment ID
- **Seller Earnings**: Query by seller ID
- **Platform Revenue**: Monitor platform fees

## 🔒 Security

### PayPal Integration
- **Secure API Calls**: OAuth 2.0 authentication
- **Webhook Verification**: Signature validation (production)
- **PCI Compliance**: No card data stored locally

### Data Protection
- **Input Validation**: Comprehensive request validation
- **SQL Injection Protection**: JPA/Hibernate protection
- **Error Handling**: Secure error responses

## 🎯 Key Benefits

1. **Automated Payment Splitting**: Seamless revenue sharing between platform and sellers
2. **Production-Ready**: Comprehensive error handling and monitoring
3. **Scalable Architecture**: Spring Boot microservice design
4. **Real-Time Processing**: Immediate payment status updates
5. **Multi-Payment Support**: Ready for Stripe, Apple Pay, Google Pay integration
6. **3D Printing Integration**: Unique STL file processing capabilities

## 📞 Support

For technical support or questions about the payment integration:
- Check application logs for detailed error information
- Monitor PayPal sandbox dashboard for payment status
- Use health endpoints for system status verification

---

**Ready for Production**: This implementation includes all necessary components for a production-ready payment processing system with automatic revenue splitting between your platform and sellers. 