
FROM python:3.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Dependencias del sistema para InsightFace, OpenCV y ODBC
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    gcc \
    g++ \
    python3-dev \
    cmake \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    unixodbc \
    unixodbc-dev \
    curl \
    ca-certificates \
    gnupg && \
    rm -rf /var/lib/apt/lists/*

# Instalar Microsoft ODBC Driver 18 para SQL Server
RUN curl -fsSL https://packages.microsoft.com/keys/microsoft.asc \
    -o /tmp/microsoft.asc && \
    gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg \
    /tmp/microsoft.asc && \
    echo "deb [signed-by=/usr/share/keyrings/microsoft-prod.gpg] https://packages.microsoft.com/debian/12/prod bookworm main" \
    > /etc/apt/sources.list.d/mssql-release.list && \
    rm /tmp/microsoft.asc && \
    apt-get update && \
    ACCEPT_EULA=Y apt-get install -y --no-install-recommends msodbcsql18 && \
    rm -rf /var/lib/apt/lists/*

# Copiar dependencias Python
COPY requirements.txt .

# Instalar FastAPI, InsightFace, OpenCV y demás librerías
RUN python -m pip install --no-cache-dir --upgrade pip setuptools wheel && \
    python -m pip install --no-cache-dir -r requirements.txt

# Copiar el código del reconocimiento facial
COPY app ./app
COPY scripts ./scripts

EXPOSE 8765

CMD ["python", "-m", "scripts.start"]
