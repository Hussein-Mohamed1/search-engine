import {SearchBar} from "@/components/searchBar";
import {SearchButtons} from "@/components/searchButtons";


export default async function Home() {

    return (<main className="flex items-center justify-center h-screen">
        <div className="flex flex-col w-screen m-auto text-center items-center space-y-6">

            {/* Lumos Logo */}
            <div className="flex flex-row text-8xl font-extrabold text-center items-center">
                <div className={"tracking-widest"}>Lumos</div>
                <div className="text-7xl">💡</div>
            </div>


            {/* Search Bar */}
            <SearchBar className={"justify-center"} width={"w-[40em]"} height={"h-[3em]"} minWidth={"20em"} variant={"home"}/>

            {/* Search Buttons */}
            <SearchButtons />
        </div>
    </main>);
}
