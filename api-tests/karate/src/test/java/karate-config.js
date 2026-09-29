function fn() {
  var baseUrl = karate.properties['karate.baseUrl'] || java.lang.System.getenv('API') || 'http://localhost:8080';
  var secret = java.lang.System.getenv('WEBHOOK_SECRET') || 'dev-only-change-me';
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 15000);
  return { baseUrl: baseUrl, secret: secret };
}
