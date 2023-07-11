# vTunnel
ポート解放をせずにVelocityプロキシへサーバーを登録できるトンネリングプラグインです。
子サーバー起動時に自動的にVelocityプロキシへサーバーを登録します。(必要に応じてforcedHostsの登録も行います。)
## 利用想定はどんな感じなの？
ポートを開放できない環境に実機サーバーとかを置いたとき<br>
**「Linode*1にVelocityプロキシ置いてるけど実機側のポート開放できないからサーバー登録できないじゃん!!!!」**<br>
こういう時にこのプラグインを使うことでポート開放をせずにサーバーをVelocityに登録することができます。<br>
<br>
頻繁にサーバーを作るとき<br>
**「サーバー立てたときVelocityにいちいち登録するのめんどすぎん?????」**<br>
vTunnelを入れておけば動的にサーバーを追加できます。<br>

## セットアップする
vTunnelのセットアップ方法。
### プロキシサイドの設定
1.vTunnelは子サーバーからのトンネリングの際に60000番ポートを使用するため、**プロキシー側でポート60000,60001番を開放**する必要があります。<br>
また、解放する必要はありませんが60002-61000ポートを内部的に使用するため他プロセスで使用しないでください。(dockerでの実行を推奨)<br>
<br>
2.VelocityプロキシのpluginsフォルダにvTunnelプラグインを導入する
3.下記のの環境変数を設定する<br>
(velocity-config.tomlのserversやtryの項目を削除することをお勧めします。)
```yaml
# JWTトークンのシークレットです。極力長い文字列にすることをお勧めします。
VTUNNEL_SECRET: """任意のシークレット文字列""" (require)

# サーバー接続時に最初に接続するサーバーの名前。
VTUNNEL_TRY: "サーバー名1,サーバー名2" (optional)
```
### サーバーサイドの設定
1.サーバーにvTunnelプラグインを導入します。<br>
2.JWTトークンを[ここ]([https://jwt.io/](https://jwt.io/#debugger-io?token=eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoi44K144O844OQ44O85ZCNKOWNiuinkuiLseaVsOWtl-ODj-OCpOODleODsygtKeOAgeOCouODs-ODgOODvOODkOODvChfKeOBruOBv-OAgikiLCJmb3JjZWRfaG9zdHMiOlsiIl0sImV4cCI6MTAwMDAwMDAwMDAwMCwiaXNzIjoidG9rZW7jga7kvZzmiJDogIUiLCJhdWQiOlsi5L2_55So6ICF5oOz5a6aKOmBqeW9k-OBq-WFpeOCjOOBpuWkp-S4iOWkq-OBp-OBmSkiXX0.nXdODR_g-sH9aB7OHxRDRDzHx-zt6YYBfUP5w7pQbDnHvu3FzYpcOTw4JC-6VJEZuNBPDMkiWaEfxEjlDbnOMQ))から生成する(下記参照)<br>
2.下記の環境変数を設定する<br>
#### JWTトークンの項目
```yaml
Algorithm: HS512

your-256-bit-secret: """上で設定した$VTUNNEL_SECRETの値"""

PAYLOAD:
    name: lobby, #(require)Velocityに登録するサーバー名
    forced_hosts: ["lobby.example.com","main.example.com"], #(optional)Velocityに登録するforcedHostsのアドレス
    exp: 1000000000000, #(require)このトークンの有効期限(unix_time/sec)
    iss: moruch4nn, #(require)このトークンの発行者(適当で構いません)
    aud: ["小サーバー用"], #(require)このトークンの想定利用者(適当で構いません)
```
### 環境変数
```yaml
# vTunnelサーバーのホスト名
VTUNNEL_HOST: 478.43.12.432
VTUNNEL_TOKEN: 上で生成したJWTToken。
```
