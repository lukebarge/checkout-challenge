# Checkout.com Payment Gateway Challenge

A Java-based payment gateway

## Prerequisites

- Java 21
- Docker 
- Gradle (or use the included Gradle wrapper)

## Getting Started


### 2. Build the application

Using Gradle wrapper:
```bash
./gradlew build
```

### 4. Run the Application

You can run the application in two ways:

#### Option 1: Using Gradle
```bash
./gradlew run
```

#### Option 2: Using Docker
```bash
docker build -t payment-gateway .
docker run -p 7070:7070 payment-gateway
```

The application will start on port 7070.

## API Endpoints

### Process Payment
- **POST** `/api/payments`
- **Headers**: 
  - `Cko-Idempotency-Key: <unique-key>` (optional) - A unique key that ensures the same payment is not processed multiple times.
- **Request Body**:
- `card_number`: Valid card number
- `expiry_month`: Card expiry month (1-12)
- `expiry_year`: Card expiry year
- `currency`: Three-letter currency code (e.g., GBP, EUR, USD)
- `amount`: Amount in minor currency units (e.g., pence, cents). For example, 1000 represents £10.00, €10.00, or $10.00
- `cvv`: Card security code

**Note**: The application uses an in-memory database to store payment records. A payment record is only persisted after passing API validation and receiving a successful response from the bank simulator. If either the validation fails or the bank request fails, no payment record will be stored

**Important**: Since bank requests do not include idempotency keys, if a connection issue occurs after sending a payment to the bank but before receiving a response, the payment gateway cannot determine if the payment was successful. In such cases, the payment status will remain unknown and no payment record will be stored.

**Validation**: When validating payment requests, the application accumulates all validation errors rather than stopping at the first error. This means the API response will include a complete list of all validation failures, allowing clients to fix multiple issues at once rather than discovering them one at a time.
```json
{
  "card_number": "2222405343248877",
  "expiry_month": 4,
  "expiry_year": 2022,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

### Retrieve Payment
- **GET** `/api/payments/{payment_id}`

## Configuration

The application can be configured through `src/main/resources/application.properties`:
- `server.port`: Application port (default: 7070)
- `bank.simulator.url`: Bank simulator URL (default: http://localhost:8080)

## Development

### Testing Strategy
The application employs two types of testing:

1. **Unit Tests**: Test individual components in isolation, mocking external dependencies.

2. **Functional Tests**: Test the entire payment flow from API request through to bank communication. These tests are particularly valuable as they verify the complete payment journey, ensuring all components work together correctly and that the application behaves as expected in real-world scenarios.

### Running Tests
```bash
./gradlew test
```
