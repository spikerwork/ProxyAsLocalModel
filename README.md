# ProxyAsLocalModel

Proxy remote LLM API as Local model. Especially works for using custom LLM in JetBrains AI Assistant.

Powered by Ktor and kotlinx.serialization. Thanks to their no-reflex features.

## Currently supported

Proxy from: OpenAI, Claude, DashScope(Alibaba Qwen), Gemini, Deepseek, Mistral, SiliconFlow.

Proxy as: LM Studio, Ollama.

Streaming chat completion API only.

## How to use

This application is a proxy server, distributed as a fat runnable jar and a GraalVM native image (Windows x64).

Run the application, and you will see a help message:

```
2025-05-02 10:43:53 INFO  Help - It looks that you are starting the program for the first time here.
2025-05-02 10:43:53 INFO  Help - A default config file is created at your_path\config.yml with schema annotation.
2025-05-02 10:43:53 INFO  Config - Config file watcher started at your_path\config.yml
2025-05-02 10:43:53 INFO  LM Studio Server - LM Studio Server started at 1234
2025-05-02 10:43:53 INFO  Ollama Server - Ollama Server started at 11434
2025-05-02 10:43:53 INFO  Model List - Model list loaded with: []
```

Then you can edit the config file to set up your proxy server.

## Config file

This config file is automatically hot-reloaded when you change it. Only the influenced parts of the server will be updated.

When first generating the config file, it will be created with schema annotations. This will bring completion and check in your editor.

## Example config file

```yaml
# $schema: https://github.com/Stream29/ProxyAsLocalModel/raw/master/config_v2.schema.json
lmStudio:
  port: 1234 # This is default value
  enabled: true # This is default value
ollama:
  port: 11434 # This is default value
  enabled: true # This is default value
client:
  socketTimeout: 1919810 # Long.MAX_VALUE is default value, in milliseconds
  connectionTimeout: 1919810 # Long.MAX_VALUE is default value, in milliseconds
  requestTimeout: 1919810 # Long.MAX_VALUE is default value, in milliseconds
  retry: 3 # This is default value
  delayBeforeRetry: 1000 # This is default value, in milliseconds

apiProviders:
  OpenAI:
    type: OpenAi
    baseUrl: https://api.openai.com/v1
    apiKey: <your_api_key>
    modelList:
      - gpt-4o
  Claude:
    type: Claude
    apiKey: <your_api_key>
    modelList:
      - claude-3-7-sonnet
  Qwen:
    type: DashScope
    apiKey: <your_api_key>
    modelList: # This is default value
      - qwen-max
      - qwen-plus
      - qwen-turbo
      - qwen-long
  DeepSeek:
    type: DeepSeek
    apiKey: <your_api_key>
    modelList: # This is default value
      - deepseek-chat
      - deepseek-reasoner
  Mistral:
    type: Mistral
    apiKey: <your_api_key>
    modelList: # This is default value
      - codestral-latest
      - mistral-large
  SiliconFlow:
    type: SiliconFlow
    apiKey: <your_api_key>
    modelList:
      - Qwen/Qwen3-235B-A22B
      - Pro/deepseek-ai/DeepSeek-V3
      - THUDM/GLM-4-32B-0414
  Gemini:
    type: Gemini
    apiKey: <your_api_key>
    modelList:
      - gemini-2.5-flash-preview-04-17
```