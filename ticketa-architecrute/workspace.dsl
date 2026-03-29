workspace "Ticketa Platform" "Production-grade event-driven ticketing system" {

    !identifiers hierarchical

    model {

        ##################################################
        # USERS
        ##################################################
        client = person "Client" "Searches events and buys tickets"
        organizer = person "Organizer" "Creates and manages events"

        ##################################################
        # EXTERNAL SYSTEMS
        ##################################################
        paymentGateway = softwareSystem "Payment Gateway" "External payment provider"
        notificationProvider = softwareSystem "Notification Provider" "Email/SMS delivery service"

        ##################################################
        # MAIN SYSTEM
        ##################################################
        ticketa = softwareSystem "Ticketa Platform" "Event-driven ticket booking system (Choreography Saga)" {

            ##################################################
            # INFRASTRUCTURE
            ##################################################
            postgres = container "PostgreSQL" "Bookings, payments, outbox (ACID)" "Database"
            mongo = container "MongoDB" "Event catalog (read-optimized)" "Database"
            redis = container "Redis" "Distributed locks (SET NX + TTL), caching" "Cache"
            kafka = container "Kafka" "Event streaming platform (at-least-once, DLQ)" "Message Broker"
            minio = container "MinIO" "Ticket PDF storage" "Object Storage"

            ##################################################
            # EDGE
            ##################################################
            gateway = container "API Gateway" "Routing, JWT validation, rate limiting, circuit breaker" "Spring Cloud Gateway"

            auth = container "Auth Service" "Authentication, JWT issuing" "Spring Boot"

            ##################################################
            # CATALOG (READ-HEAVY)
            ##################################################
            catalog = container "Event Catalog Service" "Event search (CQRS read model)" "Spring Boot" {
                catalogService = component "Catalog Service"
                catalogRepository = component "Catalog Repository"

                catalogService -> catalogRepository
                catalogRepository -> mongo
            }

            ##################################################
            # BOOKING (WRITE CORE)
            ##################################################
            booking = container "Booking Service" "Seat reservation + saga initiator" "Spring Boot" {
                controller = component "Booking Controller"
                service = component "Booking Service"
                lockManager = component "Seat Lock Manager" "Redis SET NX + TTL"
                repository = component "Booking Repository"
                outbox = component "Outbox Publisher" "Transactional outbox"

                controller -> service
                service -> lockManager
                service -> repository
                service -> outbox

                repository -> postgres
                lockManager -> redis
                outbox -> kafka
            }

            ##################################################
            # PAYMENT
            ##################################################
            payment = container "Payment Service" "Processes payments (idempotent)" "Spring Boot" {
                consumer = component "Kafka Consumer"
                processor = component "Payment Processor" "Idempotent"
                outbox = component "Outbox Publisher"

                consumer -> processor
                processor -> paymentGateway
                processor -> outbox

                consumer -> kafka
                outbox -> kafka
            }

            ##################################################
            # TICKET (PDF)
            ##################################################
            ticket = container "Ticket Service" "Generates ticket PDFs" "Spring Boot" {
                consumer = component "Kafka Consumer"
                generator = component "PDF Generator"
                storage = component "Storage Service"

                consumer -> generator
                generator -> storage
                storage -> minio

                consumer -> kafka
            }

            ##################################################
            # NOTIFICATION
            ##################################################
            notification = container "Notification Service" "Sends email/SMS (idempotent)" "Spring Boot" {
                consumer = component "Kafka Consumer"
                notifier = component "Notification Sender" "Idempotent"

                consumer -> notifier
                notifier -> notificationProvider

                consumer -> kafka
            }

            ##################################################
            # SYNCHRONOUS
            ##################################################
            gateway -> auth "Authenticate"
            gateway -> catalog "Search events"
            gateway -> booking "Create booking"

            ##################################################
            # EVENT STREAMS (SAGA)
            ##################################################
            booking -> kafka "booking-created"
            booking -> kafka "booking-cancelled"

            kafka -> payment "consume booking-created"

            payment -> kafka "payment-success"
            payment -> kafka "payment-failed"

            kafka -> booking "consume payment-failed (release seats)"

            kafka -> ticket "consume payment-success"
            kafka -> notification "consume payment-success"

        }

        ##################################################
        # USER INTERACTION
        ##################################################
        client -> ticketa.gateway "Browse & purchase tickets"
        organizer -> ticketa.gateway "Manage events"

    }

    ##################################################
    # VIEWS
    ##################################################
    views {

        systemContext ticketa {
            include *
            autoLayout lr
        }

        container ticketa {
            include *
            autoLayout lr
        }

        component ticketa.booking {
            include *
            autoLayout tb
        }

        component ticketa.payment {
            include *
            autoLayout tb
        }

        component ticketa.ticket {
            include *
            autoLayout tb
        }

        component ticketa.notification {
            include *
            autoLayout tb
        }

        ##################################################
        # DYNAMIC VIEW (SAGA FLOW)
        ##################################################
        dynamic ticketa "TicketPurchaseSaga" "Choreography Saga" {

            client -> ticketa.gateway "POST /bookings"
            ticketa.gateway -> ticketa.booking "Create booking"

            ticketa.booking -> ticketa.redis "Lock seats"
            ticketa.booking -> ticketa.postgres "Persist booking"
            ticketa.booking -> ticketa.kafka "Publish booking-created"

            ticketa.kafka -> ticketa.payment "Consume booking-created"
            ticketa.payment -> paymentGateway "Process payment"

            ticketa.payment -> ticketa.kafka "Publish payment-success"

            ticketa.kafka -> ticketa.ticket "Generate ticket"
            ticketa.ticket -> ticketa.minio "Store PDF"

            ticketa.kafka -> ticketa.notification "Send notification"
            ticketa.notification -> notificationProvider "Deliver email/SMS"

            autoLayout lr
        }

        ##################################################
        # STYLES
        ##################################################
        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }

            element "Container" {
                background #438dd5
                color #ffffff
            }

            element "Database" {
                shape cylinder
                background #f5da81
            }

            element "Message Broker" {
                shape pipe
                background #d46a6a
                color #ffffff
            }
        }

        theme default
    }
}