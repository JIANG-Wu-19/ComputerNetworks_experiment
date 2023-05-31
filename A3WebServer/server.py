# 实验A.3：简单Web服务器端实现

import socket


# 定义 handle_request 函数，用于处理客户端请求
def handle_request(client_socket):
    # 接收客户端请求数据从客户端套接字接收最多 1024 字节的数据，并将其解码为字符串，存储在变量 request_data 中，
    request_data = client_socket.recv(1024).decode()

    # 解析请求数据，获取请求文件路径对这个字符串进行分割操作，将其按照回车换行符（\r\n）进行切割，将其拆分成多行。
    request_lines = request_data.split('\r\n')
    if len(request_lines) > 0:
        # 获取请求方法和文件路径
        # method：表示请求方法，如 GET、POST、PUT 等。它是请求行中的第一个部分。
        # path：表示请求的路径，即请求访问的资源在服务器上的位置。它是请求行中的第二个部分。
        method, path, _ = request_lines[0].split(' ')
        if method == 'GET':
            if path == '/':
                # 默认返回 index.html 文件
                file_path = 'index.html'
            else:
                # 构造文件路径
                file_path = path[1:]  # 去除路径中的斜杠

            try:
                # 读取文件内容
                with open(file_path, 'rb') as file:
                    file_content = file.read()
                # 构造响应报文将响应头和文件内容拼接起来，构成完整的响应数据。
                # response_data 变量通过将 response_headers 编码为字节流，并与 file_content 拼接在一起，得到最终的响应数据。
                response_headers = 'HTTP/1.1 200 OK\r\n\r\n'
                response_data = response_headers.encode() + file_content
            except FileNotFoundError:
                # 请求的文件不存在，返回 404 Not Found 错误
                response_headers = 'HTTP/1.1 404 Not Found\r\n\r\n'
                response_data = response_headers.encode()

            # 发送响应数据给客户端
            client_socket.sendall(response_data)
        else:
            response_headers = 'HTTP/1.1 405 Method Not Allowed\r\n\r\n'
            response_data = response_headers.encode()
            client_socket.sendall(response_data)

    # 关闭客户端连接
    client_socket.close()


# 定义 run_server 函数，用于启动服务器
def run_server():
    # 创建服务器套接字.
    # socket.AF_INET 参数表示使用 IPv4 地址族，socket.SOCK_STREAM 参数表示使用 TCP 协议。
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # 调用 server_socket.bind(('localhost', 80)) 绑定服务器的主机地址和端口号。'localhost' 表示服务器在本地主机上运行，80 是服务器的端口号
    server_socket.bind(('localhost', 80))

    # 调用 server_socket.listen(1) 开始监听客户端的连接请求。参数 1 表示允许同时处理的最大连接数为 1。
    server_socket.listen(1)
    print('Server is running on http://localhost:80')

    # 服务器进入循环，不断地接收客户端的连接请求
    while True:
        # 等待客户端连接
        client_socket, addr = server_socket.accept()
        print('Client connected:', addr)
        # 处理客户端请求
        handle_request(client_socket)


run_server()
