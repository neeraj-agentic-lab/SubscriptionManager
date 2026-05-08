# FitNesse Functional Tests

This module provides FitNesse-based functional flow testing for the Subscription Manager API.

## 📋 Overview

FitNesse is a standalone testing framework that allows you to write acceptance tests in wiki format. This module:

- ✅ Runs independently from the main API
- ✅ Can be enabled/disabled via configuration
- ✅ Provides fixtures for testing subscription flows
- ✅ Supports both manual and automated test execution
- ✅ Integrates with CI/CD pipelines

## 🚀 Quick Start

### Prerequisites

1. **API Server Running**: The subscription-api must be running on `http://localhost:8080`
2. **Database**: PostgreSQL must be accessible
3. **Java 17**: Required for running the tests

### Start FitNesse Server

```bash
# From project root
./gradlew :apps:fitnesse-tests:bootRun

# Or using the custom task
./gradlew :apps:fitnesse-tests:startFitNesse
```

The FitNesse server will start on **http://localhost:9090**

### Access FitNesse Wiki

Open your browser and navigate to:
```
http://localhost:9090
```

## 📁 Project Structure

```
apps/fitnesse-tests/
├── build.gradle                    # Gradle build configuration
├── src/main/
│   ├── java/com/subscriptionengine/fitnesse/
│   │   ├── FitNesseTestApplication.java    # Main application
│   │   ├── config/
│   │   │   ├── FitNesseConfiguration.java  # Auto-configuration
│   │   │   └── FitNesseProperties.java     # Configuration properties
│   │   ├── server/
│   │   │   └── FitNesseServer.java         # Server wrapper
│   │   ├── fixtures/
│   │   │   ├── SubscriptionFixture.java    # Subscription test fixture
│   │   │   └── PlanFixture.java            # Plan test fixture
│   │   └── util/
│   │       └── ApiClient.java              # REST API client
│   └── resources/
│       └── application.yml                  # Configuration
└── FitNesseRoot/                           # Wiki pages (created on first run)
```

## 🎯 Configuration

### application.yml

```yaml
fitnesse:
  enabled: true                              # Enable/disable FitNesse
  port: 9090                                 # FitNesse server port
  root-path: FitNesseRoot                    # Wiki root directory
  
  api:
    base-url: http://localhost:8080/api      # API under test
    timeout: 30000                           # Request timeout (ms)
  
  test-data:
    cleanup-after-test: true                 # Clean up test data
    use-test-database: false                 # Use separate test DB
```

### Environment Variables

You can override configuration via environment variables:

```bash
export API_BASE_URL=http://localhost:8080/api
export FITNESSE_PORT=9090
./gradlew :apps:fitnesse-tests:bootRun
```

## 📝 Writing Tests

### Example: Subscription Creation Test

Create a new wiki page in FitNesse with the following content:

```
!define TEST_SYSTEM {slim}

!path /path/to/fitnesse-tests/build/libs/*.jar

!|import|
|com.subscriptionengine.fitnesse.fixtures|

!|Subscription Fixture|
|set tenant id|tenant-123|
|set customer id|customer-456|
|set plan id|plan-basic-monthly|
|create subscription|true|
|subscription status|ACTIVE|
|has next billing date|true|
```

### Available Fixtures

#### SubscriptionFixture

Methods for testing subscription flows:

- `setTenantId(String)` - Set tenant context
- `setCustomerId(String)` - Set customer ID
- `setPlanId(String)` - Set plan ID
- `createSubscription()` - Create a new subscription
- `getSubscription(String)` - Retrieve subscription by ID
- `cancelSubscription()` - Cancel the subscription
- `renewSubscription()` - Renew the subscription
- `statusIs(String)` - Verify subscription status
- `hasNextBillingDate()` - Check if next billing date exists
- `planIdIs(String)` - Verify plan ID

#### PlanFixture

Methods for testing plan management:

- `createPlan(String, String, double)` - Create a new plan
- `getPlan(String)` - Retrieve plan by ID
- `planName()` - Get plan name
- `planPrice()` - Get plan price

## 🧪 Running Tests

### Manual Execution

1. Start FitNesse server
2. Navigate to http://localhost:9090
3. Create or edit test pages
4. Click "Test" button to run tests

### Automated Execution

```bash
# Run all tests
./gradlew :apps:fitnesse-tests:runFitNesseTests

# Run specific test suite
./gradlew :apps:fitnesse-tests:runFitNesseTests -Dtest.suite=SubscriptionTests
```

### CI/CD Integration

Add to your CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Run FitNesse Tests
  run: |
    ./gradlew :apps:subscription-api:bootRun &
    sleep 10
    ./gradlew :apps:fitnesse-tests:runFitNesseTests
```

## 🔧 Enable/Disable FitNesse

### Disable for Production

**Method 1: Configuration**
```yaml
fitnesse:
  enabled: false
```

**Method 2: Don't Deploy**

Simply don't deploy the `fitnesse-tests` module to production:

```bash
# Build only API and Worker
./gradlew :apps:subscription-api:build
./gradlew :apps:subscription-worker:build
```

**Method 3: Separate Profile**

```bash
# Development with FitNesse
./gradlew :apps:fitnesse-tests:bootRun

# Production without FitNesse
./gradlew :apps:subscription-api:bootRun
```

## 📊 Test Organization

### Recommended Structure

```
FitNesseRoot/
├── FrontPage/                      # Home page
├── SubscriptionTests/              # Subscription test suite
│   ├── CreateSubscription/
│   ├── CancelSubscription/
│   ├── RenewSubscription/
│   └── SuiteSetUp/
├── PlanTests/                      # Plan test suite
│   ├── CreatePlan/
│   ├── UpdatePlan/
│   └── DeletePlan/
└── BillingTests/                   # Billing test suite
    ├── GenerateInvoice/
    └── ProcessPayment/
```

## 🎨 Best Practices

1. **Isolation**: Each test should be independent
2. **Cleanup**: Use `SuiteTearDown` to clean up test data
3. **Fixtures**: Keep fixtures simple and focused
4. **Documentation**: Document test scenarios in wiki pages
5. **Versioning**: Keep test pages in version control (optional)

## 🐛 Troubleshooting

### FitNesse Won't Start

```bash
# Check if port is in use
lsof -i:9090

# Kill existing process
kill -9 $(lsof -ti:9090)

# Restart
./gradlew :apps:fitnesse-tests:bootRun
```

### API Connection Errors

- Verify API is running: `curl http://localhost:8080/api/actuator/health`
- Check `fitnesse.api.base-url` configuration
- Review logs for authentication issues

### Test Failures

- Check API logs for errors
- Verify test data exists in database
- Ensure proper authentication tokens
- Review fixture implementation

## 📚 Resources

- [FitNesse Official Documentation](http://fitnesse.org/)
- [Slim Test System](http://fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests.SliM)
- [Writing Acceptance Tests](http://fitnesse.org/FitNesse.UserGuide.WritingAcceptanceTests)

## 🔐 Security Notes

- **Never run FitNesse in production**
- **Use test credentials only**
- **Isolate test database from production**
- **Disable in production deployments**

## 📈 Metrics

FitNesse provides built-in test metrics:

- Test execution time
- Pass/fail counts
- Historical test results
- Test coverage reports

Access metrics at: `http://localhost:9090/TestHistory`

## 🤝 Contributing

When adding new fixtures:

1. Create fixture class in `fixtures/` package
2. Extend `ApiClient` for API interactions
3. Document methods in README
4. Create example wiki pages
5. Add integration tests

## 📝 License

Same as parent project.
