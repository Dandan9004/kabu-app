from kabu import trending
from kabu.validate import validate_evaluations

NAMES = {"1967": "ヤマト", "9064": "ヤマトホールディングス", "9064#0": "ヤマト運輸", "4912": "ライオン", "6758": "ソニーグループ", "6758#0": "ソニーG"}


def test_longest_name_wins():
    assert trending.find_companies("ヤマト運輸が値上げを発表", NAMES) == ["9064#0"]


def test_everyday_word_needs_a_business_headline():
    about = trending.headline_filter("4912", NAMES)
    assert not about("動物園のライオンに赤ちゃん")
    assert about("ライオン、通期の利益予想を上方修正")


def test_pick_caps_each_industry_at_five(monkeypatch):
    companies = {f"{i:04d}": {"sector": "電気機器"} for i in range(1, 8)}
    companies["0100"] = {"sector": "銀行業"}
    counts = {code: 100 - int(code) for code in companies}
    monkeypatch.setattr(trending, "industry_group", lambda code, sector: sector)
    picked = trending.pick(counts, companies)
    assert sum(companies[c]["sector"] == "電気機器" for c in picked) == 5
    assert "0100" in picked


def evaluation(code="0001", power=4, cheap=2, scores=(5, 4, 4, 3, 4)):
    roles = ["事業の目利き", "決算の読み手", "成長の見張り", "業界と景気", "株主目線"]
    experts = [{"role": r, "tag": "power", "score": s, "why": "理由がここに十文字以上入る"} for r, s in zip(roles, scores)]
    experts.append({"role": "値段の番人", "tag": "cheapness", "score": cheap, "why": "利益の何倍かを業種のふつうと比べた"})
    return {"code": code, "power": power, "cheapness": cheap, "headline": "強い会社。ただ今は高め",
            "summary": "製造装置の強みは本物で、決算も伸びている。ただ値段は業種のふつうより高い。",
            "weakness": {"severity": "中", "points": ["お客さんが数社に偏っている", "株価がこの半年で2倍になった", "輸出の規制が広がるおそれ"]},
            "experts": experts}


def test_good_evaluation_passes():
    assert validate_evaluations({"date": "2026-11-14", "items": [evaluation()]}, ["0001"]) == []


def test_power_far_from_experts_is_rejected():
    errors = validate_evaluations({"date": "2026-11-14", "items": [evaluation(power=1)]}, ["0001"])
    assert any("more than 1 away" in e for e in errors)


def test_advice_words_are_rejected():
    item = evaluation()
    item["headline"] = "今が買い時の会社"
    errors = validate_evaluations({"date": "2026-11-14", "items": [item]}, ["0001"])
    assert any("買い" in e for e in errors)


def test_missing_pick_is_rejected():
    errors = validate_evaluations({"date": "2026-11-14", "items": [evaluation()]}, ["0001", "0002"])
    assert any("do not match" in e for e in errors)
