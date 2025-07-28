# Contributing to Order Service

Thank you for your interest in contributing to Order Service! This guide will help you get started.

## 🚀 Quick Start

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/order-service.git
   cd order-service
   ```
3. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 🛠️ Development Setup

### Prerequisites
- Java 21 (OpenJDK or Oracle JDK)
- Docker Desktop
- Git
- AWS CLI (for deployment testing)

### Local Development

1. **Install dependencies and run tests**:
   ```bash
   ./gradlew build
   ```

2. **Start the application**:
   ```bash
   ./gradlew bootRun
   ```

3. **Test the endpoints**:
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/orders
   ```

### Docker Development

1. **Build the image**:
   ```bash
   docker build -t order-service .
   ```

2. **Run the container**:
   ```bash
   docker run -p 8080:8080 order-service
   ```

## 📝 Making Changes

### Code Style
- Follow Java naming conventions
- Use Spring Boot best practices
- Add meaningful comments for complex logic
- Keep methods small and focused
- Use descriptive variable names

### Commit Messages
Use conventional commits format:
```
type(scope): description

Examples:
feat(api): add new order status endpoint
fix(health): resolve database connection check
docs(readme): update API documentation
test(orders): add unit tests for order calculation
```

### Testing
- Write unit tests for new features
- Ensure all existing tests pass
- Test manually with curl commands
- Verify Docker build works

## 🔄 Pull Request Process

1. **Update documentation** if needed
2. **Add or update tests** for your changes
3. **Ensure CI pipeline passes**
4. **Create a pull request** using the provided template
5. **Wait for code review**

### PR Requirements
- [ ] All tests pass
- [ ] Code is documented
- [ ] No breaking changes (unless discussed)
- [ ] Branch is up to date with main
- [ ] Descriptive title and description

## 🧪 Testing Guidelines

### Unit Tests
- Test business logic thoroughly
- Mock external dependencies
- Use meaningful test names
- Follow AAA pattern (Arrange, Act, Assert)

### Integration Tests
- Test complete request/response flows
- Verify error handling
- Test health check endpoints

### Manual Testing
```bash
# Test all endpoints
curl http://localhost:8080/health
curl http://localhost:8080/health/ready
curl http://localhost:8080/orders
curl http://localhost:8080/orders/calculate
curl http://localhost:8080/nonexistent  # Should return 404
```

## 🚀 Deployment

### Local Testing
Always test your changes locally before submitting:
```bash
# Build and test
./gradlew clean build

# Run locally
./gradlew bootRun

# Test in Docker
docker build -t order-service-test .
docker run -p 8080:8080 order-service-test
```

### AWS Deployment
Only maintainers can deploy to AWS. The CI/CD pipeline automatically deploys main branch changes.

## 📋 Issue Reporting

### Bug Reports
Include:
- Steps to reproduce
- Expected behavior
- Actual behavior
- Environment details
- Error logs/screenshots

### Feature Requests
Include:
- Clear description of the feature
- Use case/business value
- Proposed API changes
- Backwards compatibility considerations

## 💡 Getting Help

- **Documentation**: Check README.md first
- **Issues**: Search existing issues before creating new ones
- **Questions**: Create a GitHub issue with the 'question' label

## 🏷️ Labels

We use these labels for issues and PRs:
- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Improvements to documentation
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention is needed
- `question` - Further information is requested

## 📄 Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Follow GitHub's community guidelines

Thank you for contributing! 🎉 