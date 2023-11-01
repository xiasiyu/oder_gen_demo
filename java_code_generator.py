import json
import re

from langchain import ConversationChain
from langchain.chat_models import ChatOpenAI
from langchain.memory import ConversationBufferMemory

from src.agent.plan import plan_test
from src.generator.base import BaseGenerator
from src.parser.response_parser import extract_json_from_md

TEST_STRATEGY = {
    "controller": """
    使用 @WebMvcTest 注解来测试 Controller。这将只加载与 MVC 相关的部分，不加载整个 Spring Boot 上下文。这种方式可以快速地测试 Controller 是否正确响应请求。
    使用 MockMvc 来模拟 HTTP 请求并验证响应。
    使用 @MockBean 来模拟你的服务层的行为，这样你可以在不真正调用服务层的情况下测试 Controller。
    """,
    "repository": """
    由于 Repository 层与数据库的交互密切，所以单元测试在这里不太适用。我们需要执行集成测试来确保数据访问逻辑与实际的数据库交互正确。
    使用 @DataJpaTest 注解来进行集成测试。这会专门加载与 JPA 相关的组件，而不加载完整的 Spring Boot 上下文。
    使用嵌入式数据库（如 H2）来运行测试。这样可以在隔离的环境中进行测试，而不会影响到实际的生产数据库。
    """,
    "service": """
    Service 层的单元测试应该独立于其他组件（如数据库、外部服务等）。
    使用 Mocking 框架（如 Mockito）来模拟依赖项，如 Repository 或其他服务。
    验证 Service 是否正确调用了其依赖项，并返回了预期的结果。
    """,
    "other": "general unit test strategy",
}

BASE_PROMPT = """
Now you are a java programmer, i will give you json information of java file which is one file of springboot project.
Then you can write tests based on this file
i will tell you the folder structure of the project, and the code of the java file.
you need to write tests for this java program.

## here is the code:
{code}

this code is {layer} layer code
for this code, the test strategy is
{strategy}

## test cases you need to write
{test_cases}

## output format
your return should be a json like:
{{
    "res": {{
        "code": "generated java code",
        "name": "test.java"
    }}
}}
[One content should be in markdown format.]
===========
just give me the json result, no explanation
===========
"""


class JavaCodeGenerator(BaseGenerator):
    def __init__(self, llm=None):
        self.llm = llm
        if self.llm is None:
            self.llm = self.get_default_llm()
        self.last_prompt = None
        self.last_response = None
        self.memory = ConversationBufferMemory()
        self.chain = ConversationChain(
            llm=self.llm, verbose=True, memory=ConversationBufferMemory()
        )

    def generate(self, data: str, folder_info: str = "", layer: str = "other") -> any:
        test_cases = plan_test(data)
        if len(test_cases) == 0:
            return None
        prompt = BASE_PROMPT.format(
            code=data,
            strategy=TEST_STRATEGY[layer],
            folder=folder_info,
            layer=layer,
            test_cases=test_cases,
        )
        res = self.chain.predict(input=prompt)
        return self.parse_json_from_llm_response(res)

    def retry_generate(self, error, history) -> dict:
        prompt = """
        I need you to fix an error in a unit test, an error occurred while compiling and executing.
        ## here is the error:
        {error}

        ## here is the code:
        {history}

        Please fix the error and return the whole fixed unit test.
        You can use Junit 5, Mockito 3 and reflection.

        ## output format
        your return should be a json like:
        {{
        "res": {{
        "code": "generated java code",
        "name": "test.java"
        }}
        }}
        [on content should be in markdown format.]
        ===========
        just give me the json result, no explanation
        ===========
        """
        prompt_template = prompt.format(error=error, history=history)
        res = self.chain.predict(input=prompt_template)
        return self.parse_json_from_llm_response(res)

    @staticmethod
    def get_default_llm() -> ChatOpenAI:
        chat = ChatOpenAI(temperature=0, model_name="gpt-4-0613")
        return chat

    @staticmethod
    def parse_json_from_llm_response(response: str) -> dict:
        try:
            json_obj = json.loads(response, strict=False)

            # Extract the code block from the JSON
            code_block = json_obj["res"]["code"]

            # Use a regular expression to remove the markdown
            cleaned_code_block = re.sub(
                r"```.*?\n|```", "", code_block, flags=re.DOTALL
            )

            # Update the code block in the JSON
            json_obj["res"]["code"] = cleaned_code_block

            return json_obj
        except Exception as exc:
            res = extract_json_from_md(response)
            if res is None:
                return {"res": {"code": response, "name": "error.txt"}}


if __name__ == "__main__":
    pass
