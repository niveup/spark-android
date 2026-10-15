-- CreateTable
CREATE TABLE "books" (
    "id" TEXT NOT NULL,
    "source" TEXT NOT NULL,
    "sourceId" TEXT,
    "title" TEXT NOT NULL,
    "author" TEXT,
    "language" TEXT NOT NULL DEFAULT 'en',
    "coverUrl" TEXT,
    "isPublicDomain" BOOLEAN NOT NULL DEFAULT true,
    "textUrl" TEXT,
    "audioUrl" TEXT,
    "license" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "books_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "feed_items" (
    "id" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "bodyMd" TEXT NOT NULL,
    "actionLine" TEXT NOT NULL DEFAULT '',
    "topicTags" TEXT[],
    "language" TEXT NOT NULL DEFAULT 'en',
    "estSeconds" INTEGER NOT NULL DEFAULT 45,
    "bookId" TEXT,
    "sourceType" TEXT NOT NULL,
    "sourceRef" TEXT NOT NULL,
    "license" TEXT NOT NULL,
    "isAiGenerated" BOOLEAN NOT NULL,
    "isGrounded" BOOLEAN NOT NULL,
    "modelUsed" TEXT,
    "contentHash" TEXT NOT NULL,
    "quizJson" JSONB,
    "audioUrl" TEXT,
    "durationMs" INTEGER,
    "status" TEXT NOT NULL DEFAULT 'draft',
    "publishedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "feed_items_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "item_state" (
    "id" TEXT NOT NULL,
    "itemId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "seenAt" TIMESTAMP(3),
    "saved" BOOLEAN NOT NULL DEFAULT false,
    "liked" BOOLEAN,
    "dismissed" BOOLEAN NOT NULL DEFAULT false,
    "dwellMs" INTEGER NOT NULL DEFAULT 0,
    "quizCorrect" BOOLEAN,
    "clientEventIds" TEXT[] DEFAULT ARRAY[]::TEXT[],
    "srsDueAt" TIMESTAMP(3),
    "srsIntervalDays" INTEGER,
    "srsEase" DOUBLE PRECISION,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "item_state_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "feed_sessions" (
    "id" TEXT NOT NULL,
    "deviceId" TEXT NOT NULL,
    "cardLimit" INTEGER NOT NULL,
    "minuteLimit" INTEGER NOT NULL,
    "startedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "endedAt" TIMESTAMP(3),
    "itemsSeen" INTEGER NOT NULL DEFAULT 0,
    "limitReached" BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT "feed_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "device_tokens" (
    "id" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "deviceLabel" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "lastUsedAt" TIMESTAMP(3),
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "revokedAt" TIMESTAMP(3),

    CONSTRAINT "device_tokens_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ingest_jobs" (
    "id" TEXT NOT NULL,
    "source" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'running',
    "startedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "finishedAt" TIMESTAMP(3),
    "itemsCreated" INTEGER NOT NULL DEFAULT 0,
    "error" TEXT,

    CONSTRAINT "ingest_jobs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "feed_items_contentHash_key" ON "feed_items"("contentHash");

-- CreateIndex
CREATE UNIQUE INDEX "item_state_tokenHash_itemId_key" ON "item_state"("tokenHash", "itemId");

-- CreateIndex
CREATE UNIQUE INDEX "device_tokens_tokenHash_key" ON "device_tokens"("tokenHash");

-- AddForeignKey
ALTER TABLE "feed_items" ADD CONSTRAINT "feed_items_bookId_fkey" FOREIGN KEY ("bookId") REFERENCES "books"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "item_state" ADD CONSTRAINT "item_state_itemId_fkey" FOREIGN KEY ("itemId") REFERENCES "feed_items"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
