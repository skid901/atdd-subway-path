package nextstep.subway.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static nextstep.subway.acceptance.LineSteps.*;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("지하철 구간 관리 기능")
class LineSectionAcceptanceTest extends AcceptanceTest {

    private Long 강남역;
    private Long 양재역;
    private Long 정자역;
    private Long 판교역;

    private Long 신분당선;

    /**
     * Given 지하철역과 노선 생성을 요청 하고
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = 지하철역_생성_요청("강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청("양재역").jsonPath().getLong("id");
        정자역 = 지하철역_생성_요청("정자역").jsonPath().getLong("id");
        판교역 = 지하철역_생성_요청("판교역").jsonPath().getLong("id");

        Map<String, String> lineCreateParams = createLineCreateParams(강남역, 양재역);
        신분당선 = 지하철_노선_생성_요청(lineCreateParams).jsonPath().getLong("id");
    }

    /**
     * When 지하철 노선에 새로운 구간 추가를 요청 하면
     * Then 노선에 새로운 구간이 추가 된다
     */
    @DisplayName("지하철 노선에 구간을 등록")
    @Test
    void addLineSection() {
        // when
        지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(양재역, 정자역));

        // then
        ExtractableResponse<Response> response = 지하철_노선_조회_요청(신분당선);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(강남역, 양재역, 정자역);
    }

    /**
     * When 지하철 노선에 기존 구간에 속하지 않은 역 으로만 새로운 구간 추가를 요청 하면
     * Then 노선에 새로운 구간이 추가 실패 된다.
     */
    @DisplayName("지하철 노선에 구간을 등록 시 예외")
    @Test
    void addLineSectionException() {
        // when
        int 지하철_구간_생성_요청_응답_코드 = 지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(정자역, 판교역)).statusCode();

        // then
        ExtractableResponse<Response> response = 지하철_노선_조회_요청(신분당선);
        assertAll(
                () -> assertThat(지하철_구간_생성_요청_응답_코드).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(강남역, 양재역)
        );
    }

    /**
     * When 지하철 노선에 새로운 구간 추가를 요청 하면
     * When 지하철 노선을 요청 하면
     * Then 지하철 노선의 구간에 속한 역 목록이 조회 된다.
     */
    @DisplayName("지하철 노선의 구간에 속한 역 목록을 조회")
    @Test
    void getStations() {
        // when
        지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(양재역, 판교역)).statusCode();
        지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(판교역, 정자역)).statusCode();

        // then
        ExtractableResponse<Response> response = 지하철_노선_조회_요청(신분당선);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(강남역, 양재역, 판교역, 정자역)
        );
    }

    /**
     * Given 지하철 노선에 새로운 구간 추가를 요청 하고
     * When 지하철 노선의 마지막 구간 제거를 요청 하면
     * Then 노선에 구간이 제거된다
     */
    @DisplayName("지하철 노선에 구간을 제거")
    @Test
    void removeLineSection() {
        // given
        지하철_노선에_지하철_구간_생성_요청(신분당선, createSectionCreateParams(양재역, 정자역));

        // when
        지하철_노선에_지하철_구간_제거_요청(신분당선, 정자역);

        // then
        ExtractableResponse<Response> response = 지하철_노선_조회_요청(신분당선);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(강남역, 양재역);
    }

    /**
     * Given 지하철 노선에 새로운 구간 추가를 요청 하고
     * When 구간이 하나인 지하철 노선 에서 구간 제거를 요청 하면
     * Then 노선에 구간이 제거 실패 된다
     */
    @DisplayName("구간이 하나인 지하철 노선 에서 구간 제거 요청 시 예외")
    @Test
    void removeLineSectionException() {
        // when
        ExtractableResponse<Response> response = 지하철_노선에_지하철_구간_제거_요청(신분당선, 양재역);

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", 6 + "");
        return params;
    }
}
