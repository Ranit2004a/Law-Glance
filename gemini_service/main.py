import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
from google import genai
from google.genai import types

# Load environment variables
load_dotenv()

# Verify API Key
api_key = os.getenv("GEMINI_API_KEY")
if not api_key:
    raise ValueError("GEMINI_API_KEY environment variable is not set. Please set it in your .env file.")

# Initialize FastAPI App
app = FastAPI(title="Gemini Chat Service", description="Simple FastAPI proxy for Google Gemini 2.5 Flash")

# Initialize Gemini Client
client = genai.Client(api_key=api_key)

class QueryRequest(BaseModel):
    query: str

class QueryResponse(BaseModel):
    query: str
    answer: str
    source_documents: list[str] = []

@app.post("/query", response_model=QueryResponse)
async def query_gemini(request: QueryRequest):
    try:
        # System instructions to keep the model acting as a legal assistant
        system_instruction = (
            "You are a professional legal assistant specializing in Indian law (BNS, BNSS, IT Act, etc.).\n"
            "CRITICAL: Be extremely brief, concise, and direct. Answer in simple language. Avoid any unnecessary fluff, background history, or long introductory/concluding explanations.\n"
            "Reference specific sections or articles where appropriate.\n"
            "CRITICAL: Always detect the language of the user's message and respond in the exact same language.\n"
            "For example: If the user asks in Hindi (or Hinglish), reply in Hindi. If they ask in Tamil, reply in Tamil. "
            "Maintain a professional legal tone in the chosen language."
        )
        
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=request.query,
            config=types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.3,
                tools=[
                    types.Tool(
                        google_search=types.GoogleSearch()
                    )
                ]
            )
        )
        
        return QueryResponse(
            query=request.query,
            answer=response.text,
            source_documents=[]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gemini API call failed: {str(e)}")

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "model": "gemini-2.5-flash"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
