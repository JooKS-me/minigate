wrk.method = "POST"

wrk.body = '{"id": "10001","name": "jooks"}'

wrk.headers["Content-Type"] = "application/json"

function request()

return wrk.format('POST', nil, nil, body)

end